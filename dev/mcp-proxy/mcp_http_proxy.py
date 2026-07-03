#!/usr/bin/env python3
"""
MCP HTTP 代理 - 将 HTTP 接口转化为 MCP 工具供大模型调用

支持多分组配置，每个分组 = 一个独立的 MCP 端点 = RuoYi 的一条 REMOTE 记录。

配置示例 (config.json):

新格式（多分组）:
{
  "groups": {
    "system": {
      "auth": {"type": "bearer", "token": "xxx"},
      "tools": [
        {"name":"get_users","server":"192.168.1.100:8080","method":"GET","path":"/api/users","params":{"pageNum":"页码"}},
        {"name":"get_user","server":"192.168.1.100:8080","method":"GET","path":"/api/users/{id}","params":{"id":"用户ID"}}
      ]
    },
    "order": {
      "auth": {"type": "bearer", "token": "yyy"},
      "tools": [
        {"name":"get_orders","server":"192.168.1.101:8080","method":"GET","path":"/api/orders","params":{"pageNum":"页码"}}
      ]
    }
  }
}

旧格式（自动兼容为 default 分组）:
[
  {"name":"get_users","server":"192.168.1.100:8080","method":"GET",
   "path":"/api/users","params":{"pageNum":"页码","pageSize":"每页条数"}},
  ...
]

或:
{
  "auth": {"type": "bearer", "token": "xxx"},
  "tools": [...]
}

使用:
  # SSE 模式 (推荐)
  python mcp_http_proxy.py --config config.json --port 8000

  # Stdio 模式 (仅 default 分组)
  python mcp_http_proxy.py --config config.json --stdio

依赖: pip install requests flask
"""

import base64
import json
import logging
import os
import re
import sys
import traceback
from typing import Dict, List, Optional

logger = logging.getLogger('mcp-http-proxy')
logging.basicConfig(level=logging.INFO, format='%(message)s', stream=sys.stderr)

try:
    import requests as http_requests
except ImportError:
    http_requests = None

try:
    from flask import Flask
except ImportError:
    Flask = None


PROTOCOL_VERSION = "2024-11-05"


def _mask_value(value: str) -> str:
    """对敏感信息进行脱敏，保留首尾少量字符"""
    s = str(value)
    if len(s) <= 8:
        return s[:2] + "***" + s[-2:] if len(s) > 4 else "****"
    return s[:4] + "***" + s[-4:]


class McpHttpProxy:
    """将 HTTP API 包装为 MCP 工具"""

    def __init__(self, tools_config: List[Dict], auth_config: Optional[Dict] = None):
        self.tools = tools_config
        self.auth = auth_config

    def _make_tool_definitions(self) -> List[Dict]:
        result = []
        for t in self.tools:
            properties = {}
            required = []
            path_placeholders = re.findall(r'\{(\w+)\}', t.get('path', ''))

            for pname, pdesc in t.get('params', {}).items():
                properties[pname] = {'type': 'string', 'description': str(pdesc)}
                if pname in path_placeholders or pname == '__body':
                    required.append(pname)

            for pp in path_placeholders:
                if pp not in properties:
                    properties[pp] = {'type': 'string', 'description': f'路径参数: {pp}'}
                    required.append(pp)

            schema = {'type': 'object', 'properties': properties}
            if required:
                schema['required'] = required

            server = t.get('server', '')
            desc = f"{t['method']} {t.get('path', '/')}"
            if server:
                desc = f"{server} | {desc}"

            result.append({
                'name': t['name'],
                'description': desc,
                'inputSchema': schema,
            })
        return result

    def call_tool(self, tool_name: str, arguments: Dict) -> str:
        tool = next((t for t in self.tools if t['name'] == tool_name), None)
        if not tool:
            raise ValueError(f"Unknown tool: {tool_name}")

        server = tool.get('server', 'localhost:8080')
        if not server.startswith(('http://', 'https://')):
            server = f'http://{server}'

        method = tool.get('method', 'GET').upper()
        path = tool.get('path', '/')
        path_placeholders = re.findall(r'\{(\w+)\}', path)

        for key, val in arguments.items():
            placeholder = f'{{{key}}}'
            if placeholder in path:
                path = path.replace(placeholder, str(val))

        url = f'{server}{path}'
        body_data = None
        query_params = {}
        headers = {}

        for key, val in arguments.items():
            if key == '__body':
                if isinstance(val, str):
                    try:
                        body_data = json.loads(val)
                        headers['Content-Type'] = 'application/json'
                    except json.JSONDecodeError:
                        body_data = val
                else:
                    body_data = val
                    headers['Content-Type'] = 'application/json'
            elif key in path_placeholders:
                continue
            else:
                query_params[key] = val

        if http_requests is None:
            raise RuntimeError("pip install requests")

        # 应用认证：工具级 auth 覆盖组级 auth
        tool_auth = tool.get('auth') or self.auth
        if tool_auth:
            auth_type = tool_auth.get('type', '')
            if auth_type == 'bearer':
                token = tool_auth.get('token', '')
                headers['Authorization'] = f'Bearer {token}'
                logger.info(f"[MCP] Auth: Bearer {_mask_value(token)}")
            elif auth_type == 'basic':
                username = tool_auth.get('username', '')
                password = tool_auth.get('password', '')
                encoded = base64.b64encode(f'{username}:{password}'.encode()).decode()
                headers['Authorization'] = f'Basic {encoded}'
                logger.info(f"[MCP] Auth: Basic (user={username})")
            elif auth_type == 'custom':
                name = tool_auth.get('name', '')
                value = tool_auth.get('value', '')
                headers[name] = value
                logger.info(f"[MCP] Auth: Custom {name}={_mask_value(value)}")

        import urllib.parse
        full_url = f"{url}?{urllib.parse.urlencode(query_params)}" if query_params else url
        logger.info(f"[MCP] -> {method} {full_url}")

        kwargs = {'headers': headers, 'timeout': 60}
        if body_data is not None:
            kwargs['data'] = json.dumps(body_data) if isinstance(body_data, dict) else body_data
            headers['Content-Type'] = 'application/json'

        resp = http_requests.request(method, full_url, **kwargs)

        try:
            result = json.dumps(resp.json(), ensure_ascii=False, indent=2)
        except (json.JSONDecodeError, ValueError):
            result = resp.text

        logger.info(f"[MCP] <- {resp.status_code} ({len(result)} bytes)")
        return result

    def handle_jsonrpc(self, body: Dict) -> Dict:
        req_id = body.get('id')
        method = body.get('method', '')
        params = body.get('params', {})

        if method == 'initialize':
            return {
                'jsonrpc': '2.0', 'id': req_id,
                'result': {
                    'protocolVersion': PROTOCOL_VERSION,
                    'capabilities': {'tools': {'listChanged': False}},
                    'serverInfo': {'name': 'http-mcp-proxy', 'version': '1.0.0'},
                    'tools': self._make_tool_definitions(),
                },
            }
        elif method == 'tools/list':
            return {
                'jsonrpc': '2.0', 'id': req_id,
                'result': {'tools': self._make_tool_definitions()},
            }
        elif method == 'tools/call':
            try:
                result = self.call_tool(params.get('name', ''), params.get('arguments', {}))
                return {
                    'jsonrpc': '2.0', 'id': req_id,
                    'result': {'content': [{'type': 'text', 'text': result}]},
                }
            except Exception as e:
                return {
                    'jsonrpc': '2.0', 'id': req_id,
                    'error': {'code': -32000, 'message': str(e), 'data': traceback.format_exc()},
                }
        elif method == 'ping':
            return {'jsonrpc': '2.0', 'id': req_id, 'result': {}}
        else:
            return {
                'jsonrpc': '2.0', 'id': req_id,
                'error': {'code': -32601, 'message': f'未知方法: {method}'},
            }


def start_sse(groups: Dict[str, McpHttpProxy], host: str = '0.0.0.0', port: int = 8000):
    """启动 SSE 服务器，为每个分组注册独立路由

    路由规则：
      GET  /mcp/<group>         → 分组的 SSE 端点
      POST /mcp/message/<group> → 分组的消息端点
      GET  /mcp                 → 兼容旧端点，等同于 /mcp/default
      POST /mcp/message         → 兼容旧端点，等同于 /mcp/message/default
    """
    if Flask is None:
        logger.error("pip install flask")
        sys.exit(1)

    from flask import Response as FlaskResponse, request as FlaskRequest
    app = Flask(__name__)

    # 为每个分组动态注册路由
    for group_name, proxy in groups.items():
        group_suffix = f'/{group_name}' if group_name != 'default' else ''
        ep_sse = f'sse_{group_name}'
        ep_msg = f'msg_{group_name}'

        def make_sse_handler(g=group_name, p=proxy):
            def handler():
                tools = p._make_tool_definitions()

                def generate():
                    yield f"event: endpoint\ndata: /mcp/message/{g}\n\n"
                    yield f"event: tools\ndata: {json.dumps(tools, ensure_ascii=False)}\n\n"

                return FlaskResponse(generate(), mimetype='text/event-stream',
                                headers={'Cache-Control': 'no-cache', 'Connection': 'keep-alive'})
            handler.__name__ = ep_sse
            return handler

        def make_msg_handler(g=group_name, p=proxy):
            def handler():
                body = FlaskRequest.get_json(force=True, silent=True)
                if not body:
                    return {'jsonrpc': '2.0', 'error': {'code': -32700, 'message': 'Invalid JSON'}}, 400
                if 'id' not in body:
                    return '', 202
                return p.handle_jsonrpc(body)
            handler.__name__ = ep_msg
            return handler

        app.route(f'/mcp{group_suffix}', methods=['GET'], endpoint=ep_sse)(make_sse_handler())
        app.route(f'/mcp/message{group_suffix}', methods=['POST'], endpoint=ep_msg)(make_msg_handler())

    # 打印启动日志
    logger.info(f"MCP SSE 服务器已启动 (多分组模式)")
    logger.info(f"服务器地址: http://{host}:{port}")
    for group_name, proxy in groups.items():
        endpoint = f"/mcp/{group_name}" if group_name != 'default' else "/mcp"
        logger.info(f"")
        logger.info(f"[分组: {group_name}] SSE 端点: http://{host}:{port}{endpoint}")
        for t in proxy.tools:
            logger.info(f"       {t['name']}: {t['method']} {t.get('server','')}{t.get('path','/')}")
    logger.info(f"")
    logger.info(f"共 {len(groups)} 个分组, {sum(len(p.tools) for p in groups.values())} 个工具")
    app.run(host=host, port=port, threaded=True)




def start_stdio(proxy: McpHttpProxy):
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', line_buffering=True)
    sys.stdin = io.TextIOWrapper(sys.stdin.buffer, encoding='utf-8')
    logger.info(f"MCP stdio 已就绪, {len(proxy.tools)} 个工具")

    buffer = ''
    while True:
        try:
            chunk = sys.stdin.read(4096)
            if not chunk:
                break
            buffer += chunk
            while '\n' in buffer:
                line, buffer = buffer.split('\n', 1)
                line = line.strip()
                if line:
                    try:
                        req = json.loads(line)
                        resp = proxy.handle_jsonrpc(req)
                        if resp:
                            sys.stdout.write(json.dumps(resp) + '\n')
                            sys.stdout.flush()
                    except Exception as e:
                        err = {'jsonrpc': '2.0', 'id': None,
                               'error': {'code': -32700, 'message': str(e)}}
                        sys.stdout.write(json.dumps(err) + '\n')
                        sys.stdout.flush()
        except (KeyboardInterrupt, EOFError):
            break


def load_config(config_path: str) -> Dict[str, Dict]:
    """加载配置文件，返回 Dict[str, GroupConfig]

    其中 GroupConfig 结构: {"auth": {...} or None, "tools": [tool1, tool2, ...]}

    支持三种输入格式：
    1. 新格式（多分组）: {"groups": {"system": {...}, "order": {...}}}
    2. 完整格式（单分组）: {"auth": {...}, "tools": [...]}  → 自动转为 default 分组
    3. 简化格式（纯数组）: [tool1, tool2, ...]            → 自动转为 default 分组
    """
    if not os.path.exists(config_path):
        logger.error(f"配置文件不存在: {config_path}")
        sys.exit(1)

    with open(config_path, 'r', encoding='utf-8') as f:
        try:
            data = json.load(f)
        except json.JSONDecodeError as e:
            logger.error(f"JSON 解析失败: {e}")
            sys.exit(1)

    groups: Dict[str, Dict] = {}

    if isinstance(data, dict) and 'groups' in data:
        # 新格式：多分组
        raw_groups = data['groups']
        if not isinstance(raw_groups, dict):
            logger.error("'groups' 必须是 JSON 对象")
            sys.exit(1)
        for name, cfg in raw_groups.items():
            if not isinstance(cfg, dict):
                logger.error(f"分组 '{name}' 配置必须是 JSON 对象")
                sys.exit(1)
            tools = cfg.get('tools', [])
            if not isinstance(tools, list):
                logger.error(f"分组 '{name}' 的 'tools' 必须是 JSON 数组")
                sys.exit(1)
            auth = cfg.get('auth')
            _validate_tools(tools, name)
            groups[name] = {'auth': auth, 'tools': tools}
    elif isinstance(data, dict):
        # 完整格式（单分组）：带可选全局 auth
        auth = data.get('auth')
        tools = data.get('tools', [])
        if not isinstance(tools, list):
            logger.error("'tools' 必须是 JSON 数组")
            sys.exit(1)
        _validate_tools(tools, 'default')
        groups['default'] = {'auth': auth, 'tools': tools}
    elif isinstance(data, list):
        # 简化格式：纯工具数组
        _validate_tools(data, 'default')
        groups['default'] = {'auth': None, 'tools': data}
    else:
        logger.error("配置必须是 JSON 数组或对象")
        sys.exit(1)

    return groups


def _validate_tools(tools: List, group_name: str = ''):
    """校验工具列表，补充默认字段"""
    prefix = f"分组 '{group_name}' " if group_name else ""
    for i, t in enumerate(tools):
        if 'name' not in t:
            logger.error(f"{prefix}第 {i+1} 个工具缺少 'name'")
            sys.exit(1)
        t.setdefault('server', 'localhost:8080')
        t.setdefault('method', 'GET')
        t.setdefault('path', '/')
        t.setdefault('params', {})


def main():
    import argparse

    parser = argparse.ArgumentParser(
        description='MCP HTTP 代理 - 将 HTTP 接口转化为 MCP 工具供大模型调用',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
使用示例:
  1) 准备配置文件 config.json (新格式: 多分组):
     {
       "groups": {
         "system": {
           "auth": {"type": "bearer", "token": "xxx"},
           "tools": [
             {"name":"get_users","server":"192.168.1.100:8080","method":"GET",
              "path":"/api/users","params":{"pageNum":"页码"}}
           ]
         }
       }
     }
  2) 启动 SSE: python mcp_http_proxy.py --config config.json --port 8000
  3) LLM 连接: http://localhost:8000/mcp/system
  4) 旧格式自动兼容: http://localhost:8000/mcp (default 分组)
        """,
    )
    parser.add_argument('--config', '-c', required=True, help='工具配置文件')
    parser.add_argument('--port', '-p', type=int, default=8000, help='SSE 端口')
    parser.add_argument('--host', default='0.0.0.0')
    parser.add_argument('--stdio', action='store_true', help='Stdio 模式（仅 default 分组）')

    args = parser.parse_args()
    groups_config = load_config(args.config)

    # 为每个分组创建 McpHttpProxy 实例
    groups: Dict[str, McpHttpProxy] = {}
    for name, cfg in groups_config.items():
        groups[name] = McpHttpProxy(cfg['tools'], auth_config=cfg.get('auth'))

    if args.stdio:
        # Stdio 模式仅支持 default 分组
        if 'default' in groups:
            start_stdio(groups['default'])
        else:
            logger.error("Stdio 模式需要 default 分组或旧格式配置")
            sys.exit(1)
    else:
        start_sse(groups, args.host, args.port)


if __name__ == '__main__':
    main()

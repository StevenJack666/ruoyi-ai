package org.ruoyi.support.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.pagehelper.PageInfo;
import org.ruoyi.common.core.constant.HttpStatus;
import org.ruoyi.common.core.domain.R;
import org.ruoyi.common.core.utils.openapi.MyDateUtils;
import org.ruoyi.common.core.utils.openapi.MyStringUtils;
import org.ruoyi.support.constant.BaseConstants;
import org.ruoyi.support.page.PageDomain;
import org.ruoyi.support.page.TableDataInfo;
import org.ruoyi.support.page.TableSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.InitBinder;

import java.beans.PropertyEditorSupport;
import java.util.Date;
import java.util.List;

/**
 * web层通用数据处理
 *
 * @author admin
 */
public class BaseController
{
    protected final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * 将前台传递过来的日期格式的字符串，自动转化为Date类型
     */
    @InitBinder
    public void initBinder(WebDataBinder binder)
    {
        // Date 类型转换
        binder.registerCustomEditor(Date.class, new PropertyEditorSupport()
        {
            @Override
            public void setAsText(String text)
            {
                setValue(MyDateUtils.parseDate(text));
            }
        });
    }

    /**
     * 设置请求分页数据
     */
    protected void startPage()
    {
        TableSupport.startPage();
    }

    /**
     * 设置请求分页数据
     */
    protected void startPageV2(String defaultOrderBy)
    {
        TableSupport.startPage(defaultOrderBy);
    }


    /**
     * 设置请求分页数据
     */
    @SuppressWarnings({"rawtypes", "ConstantValue", "DuplicatedCode"})
    protected Page startMybatisPage()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        if(MyStringUtils.isNull(pageNum)){
            pageNum = BaseConstants.DEFAULT_PAGE_NUM;
        }

        if(MyStringUtils.isNull(pageSize)){
            pageSize = BaseConstants.DEFAULT_PAGE_SIZE;
        }

        if (MyStringUtils.isNotNull(pageNum) && MyStringUtils.isNotNull(pageSize))
        {
            return new Page(pageNum,pageSize);
        }
        return new Page();
    }

    /**
     * 响应请求分页数据
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected TableDataInfo getDataTable(List<?> list)
    {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(new PageInfo(list).getTotal());
        return rspData;
    }

    /**
     * 响应请求分页数据
     */
    protected TableDataInfo getDataTable(Page<?> list)
    {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list.getRecords());
        rspData.setTotal(list.getTotal());
        return rspData;
    }

    protected TableDataInfo getDataTable(List<?> list, int total){
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        if (total > 0){
            int fromIndex = (pageNum - 1) * pageSize;
            int endIndex = Math.min(pageNum * pageSize,list.size());
            if (fromIndex >= list.size()){
                int lastPageNum = total / pageSize + (total % pageSize != 0? 1: 0);
                fromIndex = (lastPageNum - 1) * pageSize;
            }
            list = list.subList(fromIndex,endIndex);
        }
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list);
        rspData.setTotal(total);
        return rspData;
    }

    /**
     * 响应请求分页数据
     */
    protected TableDataInfo getDataTable(PageInfo<?> list)
    {
        TableDataInfo rspData = new TableDataInfo();
        rspData.setCode(HttpStatus.SUCCESS);
        rspData.setMsg("查询成功");
        rspData.setRows(list.getList());
        rspData.setTotal(list.getTotal());
        return rspData;
    }

    protected int getPages(int total, int rows){
        return total / rows + (total % rows > 0 ? 1 : 0);
    }

    /**
     * 响应返回结果
     *
     * @param rows 影响行数
     * @return 操作结果
     */
    protected R<?> toAjax(int rows)
    {
        return rows > 0 ? R.ok() : R.fail();
    }

    /**
     * 页面跳转
     */
    public String redirect(String url)
    {
        return MyStringUtils.format("redirect:{}", url);
    }
}

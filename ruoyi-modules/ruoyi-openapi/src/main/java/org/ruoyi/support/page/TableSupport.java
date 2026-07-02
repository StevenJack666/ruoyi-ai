package org.ruoyi.support.page;

import lombok.extern.slf4j.Slf4j;
import com.github.pagehelper.PageHelper;
import org.ruoyi.common.core.utils.ServletUtils;
import org.ruoyi.common.core.utils.openapi.MyStringUtils;
import org.ruoyi.common.core.utils.sql.SqlUtil;
import org.ruoyi.support.constant.BaseConstants;
import org.springframework.util.StringUtils;

/**
 * 表格数据处理
 *
 * @author admin
 */
@Slf4j
public class TableSupport
{
    /**
     * 当前记录起始索引
     */
    public static final String PAGE_NUM = "pageNum";

    /**
     * 每页显示记录数
     */
    public static final String PAGE_SIZE = "pageSize";

    /**
     * 排序列
     */
    public static final String ORDER_BY_COLUMN = "orderByColumn";

    /**
     * 排序的方向 "desc" 或者 "asc".
     */
    public static final String IS_ASC = "isAsc";

    /**
     * 封装分页对象
     */
    public static PageDomain getPageDomain()
    {
        PageDomain pageDomain = new PageDomain();
        pageDomain.setPageNum(ServletUtils.getParameterToInt(PAGE_NUM));
        pageDomain.setPageSize(ServletUtils.getParameterToInt(PAGE_SIZE));
        pageDomain.setOrderByColumn(ServletUtils.getParameter(ORDER_BY_COLUMN));
        pageDomain.setIsAsc(ServletUtils.getParameter(IS_ASC));
        return pageDomain;
    }

    /**
     * 设置请求分页数据
     */
    public static void startPage()
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
            // 这里是规避pagehelper orderBy漏洞
            String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
            if(StringUtils.hasText(orderBy)){
                PageHelper.startPage(pageNum, pageSize, orderBy);
            }else{
                PageHelper.startPage(pageNum, pageSize);
            }
        }
    }




    /**
     * 设置请求分页数据
     */
    public static void startPage(Integer pageNum, Integer pageSize)
    {
        if(MyStringUtils.isNull(pageNum)){
            pageNum = BaseConstants.DEFAULT_PAGE_NUM;
        }

        if(MyStringUtils.isNull(pageSize)){
            pageSize = BaseConstants.DEFAULT_PAGE_SIZE;
        }

        PageHelper.startPage(pageNum, pageSize);
    }

    public static PageDomain buildPageRequest()
    {
        return getPageDomain();
    }

    public static PageDomain startPageRequest()
    {
        PageDomain pageDomain = TableSupport.buildPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        Integer pageSize = pageDomain.getPageSize();
        if(MyStringUtils.isNull(pageNum)){
            pageNum = BaseConstants.DEFAULT_PAGE_NUM;
            pageDomain.setPageNum(pageNum);
        }

        if(MyStringUtils.isNull(pageSize)){
            pageSize = BaseConstants.DEFAULT_PAGE_SIZE;
            pageDomain.setPageSize(pageSize);
        }

        return pageDomain;
    }

    public static PageDomain startPageRequestEs()
    {
        PageDomain pageDomain = startPageRequest();
        Integer pageNum = pageDomain.getPageNum();
        pageNum = pageNum<=1?0: pageNum-1;
        pageDomain.setPageNum(pageNum);
        return pageDomain;
    }

    public static void startPage(String defaultOrderBy) {
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
            // 这里是规避pagehelper orderBy漏洞
            String orderBy = SqlUtil.escapeOrderBySql(pageDomain.getOrderBy());
            if(StringUtils.hasText(orderBy)){
                PageHelper.startPage(pageNum, pageSize, orderBy);
            }else{
                PageHelper.startPage(pageNum, pageSize,defaultOrderBy);
            }
        }
    }
}

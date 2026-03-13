package net.map591.model.enumNum;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 分页实体类
 */
public class PageEntity {
    @JsonProperty( access = JsonProperty.Access.WRITE_ONLY)
    private Integer pageNum;
    @JsonProperty( access = JsonProperty.Access.WRITE_ONLY)
    private Integer pageSize;

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}

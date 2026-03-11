package net.map591.model.enumNum;

import lombok.Data;
import lombok.ToString;
import org.springframework.stereotype.Component;

/**
 * @Author zl

 **/
@Component
@Data
@ToString
public class ConfigValue {

    public static  int RECORD_ROW =20000;
    private String pageSize="10";

    public void setRecordRow(Integer recordRow){
        ConfigValue.RECORD_ROW=recordRow;
    }

    public String getPageSize() {
        return pageSize;
    }

    public void setPageSize(String pageSize) {
        this.pageSize = pageSize;
    }
}

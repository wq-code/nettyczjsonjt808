package net.map591.mapper;

import net.map591.entity.LocationData;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface LocationMapper {

    @Insert("insert into cz_gps(terminal_phone,plate_phone,msg_sn,gps_time,longitude,latitude,geom,direction,speed)\n" +
            "        values(#{terminalPhone},#{platePhone},#{msgSn},#{gpsTime},#{longitude},#{latitude},ST_GeomFromText(#{geom},4326),#{direction},#{speed})")
    int saveLocation(LocationData res);

    @Select("SELECT zdh,cph from cz_gps_cph")
    List<Map<String, String>> getTerminalAndPlateMap();
}

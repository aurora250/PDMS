package com.pdm.resident.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface StatisticsMapper {

    /**
     * 按省份聚合常住人口数据（用于中国地图） 通过 area_id → 市 → 省 三级关联查找省份名称
     */
    /**
     * parent_id 列存储的是 area_code（6位行政区划代码），而非自增 area_id 因此二级关联需通过 area_code 匹配:
     * household_area_id → area.area_id (区县) area.parent_id → area.area_code (市)
     * city.parent_id → area.area_code (省)
     */
    /**
     * 省级统计支持2级（市→省）和3级（区→市→省）两种链路。 当 area 为市级时: a=市 (parent→省), c=省 (parent=NULL),
     * p=NULL → 取 c.area_name 当 area 为区级时: a=区 (parent→市), c=市 (parent→省), p=省
     */
    @Select("SELECT " + "  COALESCE(p.area_name, c.area_name, '未知') AS name, " + "  COUNT(*) AS value, "
            + "  COUNT(CASE WHEN r.gender = '男' THEN 1 END) AS male, "
            + "  COUNT(CASE WHEN r.gender = '女' THEN 1 END) AS female " + "FROM resident r "
            + "LEFT JOIN area a ON r.household_area_id = a.area_id "
            + "LEFT JOIN area c ON a.parent_id = c.area_code::BIGINT "
            + "LEFT JOIN area p ON c.parent_id = p.area_code::BIGINT " + "WHERE r.is_deleted = 0 "
            + "GROUP BY COALESCE(p.area_name, c.area_name, '未知') " + "ORDER BY value DESC")
    List<Map<String, Object>> getProvincePopulation();

    /** 总人口数 */
    @Select("SELECT COUNT(*) FROM resident WHERE is_deleted = 0")
    long countResidents();

    /** 待处理预警数 */
    @Select("SELECT COUNT(*) FROM alert WHERE is_handled = 0 AND is_deleted = 0")
    long countPendingAlerts();

    /** 某省份下各城市人口统计（area_id 为市级时：市→省；为区级时：区→市→省） */
    @Select("SELECT " + "  a.area_name AS name, a.area_id AS area_id, " + "  COUNT(*) AS value, "
            + "  COUNT(CASE WHEN r.gender = '男' THEN 1 END) AS male, "
            + "  COUNT(CASE WHEN r.gender = '女' THEN 1 END) AS female " + "FROM resident r "
            + "JOIN area a ON r.household_area_id = a.area_id " + "JOIN area p ON a.parent_id = p.area_code::BIGINT "
            + "WHERE r.is_deleted = 0 AND p.area_name = #{provinceName} "
            + "GROUP BY a.area_name, a.area_id ORDER BY value DESC")
    List<Map<String, Object>> getCityPopulation(@Param("provinceName") String provinceName);

    /**
     * 人口流向数据（户籍迁移省份间流动统计，不限制 business_type） 直接按解析后的省份名判断是否跨省，排除省内自环
     */
    @Select("SELECT from_name, to_name, SUM(value) AS value FROM (" + " SELECT "
            + "  COALESCE(p_from.area_name, c_from.area_name) AS from_name, "
            + "  COALESCE(p_to.area_name, c_to.area_name) AS to_name, COUNT(*) AS value "
            + "FROM household_migration_request m " + "LEFT JOIN area a_from ON m.outgoing_area_id = a_from.area_id "
            + "LEFT JOIN area c_from ON a_from.parent_id = c_from.area_code::BIGINT "
            + "LEFT JOIN area p_from ON c_from.parent_id = p_from.area_code::BIGINT "
            + "LEFT JOIN area a_to ON m.incoming_area_id = a_to.area_id "
            + "LEFT JOIN area c_to ON a_to.parent_id = c_to.area_code::BIGINT "
            + "LEFT JOIN area p_to ON c_to.parent_id = p_to.area_code::BIGINT "
            + "WHERE m.is_deleted = 0 AND m.outgoing_area_id IS NOT NULL AND m.incoming_area_id IS NOT NULL "
            + "GROUP BY 1,2 " + ") sub " + "WHERE from_name IS NOT NULL AND to_name IS NOT NULL "
            + "  AND from_name != to_name " + "GROUP BY from_name, to_name " + "ORDER BY value DESC LIMIT 100")
    List<Map<String, Object>> getMigrationFlows();
}

package com.pro.utils.validator;

import com.pro.resource.annotations.Column;
import com.pro.resource.annotations.Table;
import com.pro.utils.generator.DDLgenerator;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.*;

@Log4j2
public class SchemaValidator {

    private static final String QUERY_FOR_COLUMNS = "SELECT * FROM information_schema.COLUMNS WHERE TABLE_NAME = :table_name AND TABLE_SCHEMA = :table_schema" ;
    private final String table_schema;
    private NamedParameterJdbcTemplate template;

    public SchemaValidator(DataSource dataSource, String table_schema) {
        this.template = new NamedParameterJdbcTemplate(dataSource);
        this.table_schema = table_schema;
    }

    public void validate(Set<Class<?>> entities) {
        Map<String, List<Column>> missingDbTableColumnsMap = new HashMap<>();
        try {
            Map<String, List<ColumnsDTO>> entitiesFromDB = getEntitiesFromDB(entities);

            for (Class<?> entity : entities) {
                Table tableAnnotation = entity.getAnnotation(Table.class);
                List<ColumnsDTO> columnsFromDb = entitiesFromDB.get(tableAnnotation.name());
                exemineForMissingColumn(missingDbTableColumnsMap, entity, tableAnnotation, columnsFromDb);
            }
            addMissingColumns(missingDbTableColumnsMap);
            deleteColumns(entities, entitiesFromDB);

        } catch (Exception e){
            e.printStackTrace();
        }
    }

    private void exemineForMissingColumn(Map<String, List<Column>> missingDbTableColumnsMap, Class<?> entity, Table tableAnnotation, List<ColumnsDTO> columnsFromDb) {
        for (Field field : entity.getDeclaredFields()) {
            Column columnDef = field.getAnnotation(Column.class);
            Optional<ColumnsDTO> dbColumnDef = columnsFromDb.stream().filter(e -> e.getCOLUMN_NAME().equals(columnDef.name())).findFirst();
            if(!dbColumnDef.isPresent()){
                putMissingColumn(missingDbTableColumnsMap, tableAnnotation, columnDef);
            }
        }
    }

    private void deleteColumns(Set<Class<?>> entities, Map<String, List<ColumnsDTO>> entitiesFromDB) {
        log.info("Deleting column: ");
        for (Map.Entry<String, List<ColumnsDTO>> dbEntitiesEntry : entitiesFromDB.entrySet()) {
            for (ColumnsDTO columnsDTO : dbEntitiesEntry.getValue()) {
                Class<?> targetEntity = entities.stream().filter(e -> e.getAnnotation(Table.class).name().equals(columnsDTO.getTABLE_NAME())).findFirst().get();
                if(isColumnNotExist(columnsDTO, targetEntity)){
                    deleteColumn(columnsDTO, targetEntity);
                }

            }
        }
    }

    private void deleteColumn(ColumnsDTO columnsDTO, Class<?> targetEntity) {
        log.info(columnsDTO.getCOLUMN_NAME());
        String removeColumnSql = "ALTER TABLE " + targetEntity.getAnnotation(Table.class).name() + " DROP " + columnsDTO.getCOLUMN_NAME();
        template.update(removeColumnSql, new HashMap<>());
    }

    private boolean isColumnNotExist(ColumnsDTO columnsDTO, Class<?> targetEntity) {
        return Arrays.stream(targetEntity.getDeclaredFields()).noneMatch(e -> e.getAnnotation(Column.class).name().equals(columnsDTO.getCOLUMN_NAME()));
    }

    private void addMissingColumns(Map<String, List<Column>> missingDbTableColumnsMap) {
        log.info("Missing columns: \n");
        for (Map.Entry<String, List<Column>> missingColumns : missingDbTableColumnsMap.entrySet()) {
            for (Column missingColumn : missingColumns.getValue()) {
                addColumnToDbTable(missingColumns, missingColumn);
            }
        }
    }

    private void addColumnToDbTable(Map.Entry<String, List<Column>> missingColumns, Column missingColumn) {
        log.info(missingColumn.name());
        StringBuilder builder = new StringBuilder();
        DDLgenerator.appendFieldDDL(missingColumn, builder);
        String sql = "ALTER TABLE " + missingColumns.getKey() + " ADD COLUMN " + builder.toString().replace(",", "");
        template.update(sql, new HashMap<>());
    }

    private void putMissingColumn(Map<String, List<Column>> missingTableColumnsMap, Table tableAnnotation, Column columnDef) {
        List<Column> listOfMissingColumn = missingTableColumnsMap.get(tableAnnotation.name());

        if(listOfMissingColumn == null){
            listOfMissingColumn = new LinkedList<>();
        }

        listOfMissingColumn.add(columnDef);
        missingTableColumnsMap.put(tableAnnotation.name(), listOfMissingColumn);
    }

    private Map<String, List<ColumnsDTO>> getEntitiesFromDB(Set<Class<?>> entities){
        Map<String, List<ColumnsDTO>> map = new HashMap<>();
        for (Class<?> entity : entities) {
            String tableName = entity.getAnnotation(Table.class).name();

            Map<String, String> params = new HashMap<>();
            params.put("table_schema", table_schema);
            params.put("table_name", tableName);
            List<ColumnsDTO> columnsDTO = template.query(QUERY_FOR_COLUMNS, params, new BeanPropertyRowMapper<>(ColumnsDTO.class));
            map.put(tableName, columnsDTO);
        }
        return map;
    }

    @Data
    @NoArgsConstructor
    private static class ColumnsDTO {
        private String TABLE_CATALOG;
        private String TABLE_SCHEMA;
        private String TABLE_NAME;
        private String COLUMN_NAME;
        private Long ORDINAL_POSITION;
        private String COLUMN_DEFAULT;
        private String IS_NULLABLE;
        private String DATA_TYPE;
        private Long CHARACTER_MAXIMUM_LENGTH;
        private String CHARACTER_OCTET_LENGTH;
        private Long NUMERIC_PRECISION;
        private Long NUMERIC_SCALE;
        private Long DATETIME_PRECISION;
        private String CHARACTER_SET_NAME;
        private String COLLATION_NAME;
        private String COLUMN_TYPE;
        private String COLUMN_KEY;
        private String EXTRA;
        private String PRIVILEGES;
        private String COLUMN_COMMENT;

    }
}

package generator;

import com.pro.resources.annotations.Column;
import com.pro.resources.annotations.Table;
import com.pro.resources.annotations.Type;
import com.pro.utils.Writer;

import java.lang.reflect.Field;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class DDLgenerator {

    public static void process(Set<Class<?>> entities){
        final StringBuilder fullDDL = new StringBuilder();
        entities.forEach(e-> {
            generateTableDDL(e, fullDDL);
        });
        Writer.overwriteTo("src/main/resources/schema.sql" ,fullDDL.toString());
    }

    private static void generateTableDDL(Class<?> e, StringBuilder fullDDL){
        final StringBuilder tableDDL = new StringBuilder("create table if not exists " + e.getAnnotation(Table.class).name() +" (\n");
        List<String> constraints = new LinkedList<>();

        for (Field declaredField : e.getDeclaredFields()) {
            Column definition = declaredField.getAnnotation(Column.class);
            tableDDL.append("'" + definition.name() + "'   " + typeToString(definition.type()[0]) + addOtherConstraints(definition) +",\n");
            generateKeyConstraint(definition, constraints);
        }
        appendKeyConstraints(constraints, tableDDL);
        tableDDL.replace(tableDDL.lastIndexOf(","), tableDDL.lastIndexOf(",") + 1, "").append(");\n"); //todo replace only >1 fields
        fullDDL.append(tableDDL.toString() + "\n");
    }

    private static void appendKeyConstraints(List<String> constraints, StringBuilder tableDDL) {
        for (String constraint : constraints) {
            tableDDL.append(constraint + ",\n");
        }
    }

    private static void generateKeyConstraint(Column definition, List<String> constraints) {
        if(definition.primaryKey()){
            constraints.add("PRIMARY KEY (" + definition.name() + ")");
        }

        if(definition.foreignKey().length == 1 ){
            Class aClass = definition.foreignKey()[0]
                    .targetEntity();
            constraints.add("FOREIGN KEY (" + definition.name() + ")" + " REFERENCES " + ((Table)definition.foreignKey()[0]
                    .targetEntity().getAnnotation(Table.class)).name()
                    + "(" + definition.foreignKey()[0].filedName() + ")");
        }
    }

    private static String addOtherConstraints(Column definition) {
        return definition.notNull()? "  not null" : "" + (definition.unique()? " UNIQUE" : "");
    }

    private static String typeToString(Type definition) {
        return  definition.dataType() + (definition.lenght() != -1 ? "(" + definition.lenght() + ")" : "");
    }

}
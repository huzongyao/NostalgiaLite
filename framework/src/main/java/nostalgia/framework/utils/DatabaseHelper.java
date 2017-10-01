package nostalgia.framework.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Pair;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import nostalgia.framework.ui.gamegallery.GameDescription;
import nostalgia.framework.ui.gamegallery.ZipRomFile;
import nostalgia.framework.utils.annotations.Column;
import nostalgia.framework.utils.annotations.ObjectFromOtherTable;
import nostalgia.framework.utils.annotations.Table;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String TAG = "DatabaseHelper";
    private static int DB_VERSION_CODE = 21;
    private Class<?>[] classes = new Class<?>[]{
            GameDescription.class,
            ZipRomFile.class
    };
    private HashMap<Class<?>, ClassItem> classItems = new HashMap<>();

    public DatabaseHelper(Context context) {
        super(context, "db", null, DB_VERSION_CODE);
        for (Class<?> cls : classes) {
            classItems.put(cls, new ClassItem(cls));
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            for (Class<?> cls : classes) {
                String sql = getCreateSql(cls);
                db.execSQL(sql);
                NLog.i(TAG, "sql:" + sql);
            }

        } catch (Exception e) {
            NLog.e(TAG, "", e);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion == 13 && newVersion == 21) {
            return;
        }
        removeTablesDB(db);
        onCreate(db);
    }

    private String getCreateSql(Class<?> cls) {
        ClassItem classItem = classItems.get(cls);
        String tableName = classItem.tableName;
        StringBuilder sql = new StringBuilder();
        sql.append("CREATE TABLE ").append(tableName).append(" (");

        for (Field field : cls.getDeclaredFields()) {
            Column column = field.getAnnotation(Column.class);

            if (column != null) {
                String cName = column.columnName();
                cName = cName.equals("") ? field.getName() : cName;
                Type type = field.getType();
                boolean supported = true;
                String dbType = "";

                if ((type == String.class) || ((Class<?>) type).isEnum()) {
                    dbType = "TEXT";

                } else if (type == Integer.class || type == int.class
                        || type == Long.class || type == long.class
                        || type == Boolean.class || type == boolean.class) {
                    dbType = "INTEGER";

                } else if (type == Float.class || type == float.class) {
                    dbType = "REAL";

                } else {
                    NLog.e(TAG, "type " + type + " is not supported");
                    supported = false;
                }

                if (supported) {
                    sql.append(cName).append(" ").append(dbType).append(" ");

                    if (column.isPrimaryKey()) {
                        sql.append("PRIMARY KEY ");
                    }

                    if (!column.allowNull()) {
                        sql.append("NOT NULL ");
                    }

                    if (column.unique()) {
                        sql.append("UNIQUE ");
                    }

                    sql.append(",");
                }
            }

            ObjectFromOtherTable objectFromOtherTable = field.getAnnotation(ObjectFromOtherTable.class);

            if (objectFromOtherTable != null) {
                Type fieldClass = field.getType();
                Table table2 = null;

                if (Collection.class.isAssignableFrom((Class<?>) fieldClass)) {
                    Class<?> classType = getCollectionGenericClass(field);

                    if (classType != null)
                        table2 = classType.getAnnotation(Table.class);

                } else {
                    table2 = field.getClass().getAnnotation(Table.class);
                }

                if (table2 == null) {
                    throw new RuntimeException("Field " + cls.getSimpleName() + "." + field.getName()
                            + " must refered to class with Table annotation"
                            + " or Collection with generic type with Table annotation");
                }
            }
        }

        sql.delete(sql.length() - 1, sql.length());
        sql.append(");");
        return sql.toString();
    }

    private Class<?> getCollectionGenericClass(Field field) {
        Type type = field.getGenericType();

        if (type instanceof ParameterizedType) {
            ParameterizedType type2 = (ParameterizedType) type;
            Type[] typeArguments = type2.getActualTypeArguments();
            return ((Class<?>) typeArguments[0]);

        } else {
            return null;
        }
    }

    private void removeTablesDB(SQLiteDatabase db) {
        for (Class<?> cls : classes) {
            Table table = cls.getAnnotation(Table.class);

            if (table != null) {
                String tableName = table.tableName();
                tableName = tableName.equals("") ? cls.getSimpleName() : tableName;
                db.execSQL("DROP TABLE IF EXISTS " + tableName);
                NLog.i(TAG, "delete table " + tableName);

            } else {
                throw new RuntimeException("class " + cls.getName() + " has not @Table annotation");
            }
        }
    }

    private void clearTablesDB(SQLiteDatabase db) {
        for (Class<?> cls : classes) {
            Table table = cls.getAnnotation(Table.class);

            if (table == null) {
                throw new RuntimeException("class " + cls.getName() + " has not @Table annotation");
            }

            String tableName = table.tableName();
            tableName = tableName.equals("") ? cls.getSimpleName() : tableName;
            db.execSQL("DELETE FROM " + tableName);
            NLog.i(TAG, "clear table " + tableName);
        }
    }

    public void clearDB() {
        SQLiteDatabase db = getWritableDatabase();

        try {
            clearTablesDB(db);

        } finally {
            db.close();
        }
    }

    public void updateObjToDb(Object obj, String[] fields) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            db.beginTransaction();
            updateObjToDb(obj, db, fields);
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void insertObjToDb(Object obj) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            db.beginTransaction();
            insertObjToDb(obj, db, null);
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public void insertObjsToDb(List<Object> objs) {
        SQLiteDatabase db = getWritableDatabase();

        try {
            db.beginTransaction();

            for (Object obj : objs) {
                insertObjToDb(obj, db, null);
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            db.close();
        }
    }

    public int countObjsInDb(Class<?> cls, String where) {
        where = where == null ? "" : where;
        SQLiteDatabase db = getWritableDatabase();
        int count = -1;

        try {
            db.beginTransaction();
            count = countObjsInDb(cls, db, where);
            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            db.close();
        }

        return count;
    }

    public <T> ArrayList<T> selectObjsFromDb(Class<T> cls) {
        ArrayList<T> resultList = null;
        SQLiteDatabase db = getReadableDatabase();

        try {
            resultList = selectObjsFromDb(cls, db, null, null, null, true);

        } finally {
            db.close();
        }

        return resultList;
    }

    public <T> ArrayList<T> selectObjsFromDb(Class<T> cls, boolean deep, String groupBy, String orderBy) {
        ArrayList<T> resultList = null;
        SQLiteDatabase db = getReadableDatabase();

        try {
            resultList = selectObjsFromDb(cls, db, null, groupBy, orderBy, deep);

        } finally {
            db.close();
        }

        return resultList;
    }

    public <T> T selectObjFromDb(Class<T> cls, String where) {
        ArrayList<T> resultList = null;
        SQLiteDatabase db = getReadableDatabase();

        try {
            resultList = selectObjsFromDb(cls, db, where, null, null, true);

        } finally {
            db.close();
        }

        if (resultList.isEmpty()) {
            return null;

        } else {
            return resultList.get(0);
        }
    }

    public <T> T selectObjFromDb(Class<T> cls, String where, boolean deep) {
        ArrayList<T> resultList = null;
        SQLiteDatabase db = getReadableDatabase();

        try {
            resultList = selectObjsFromDb(cls, db, where, null, null, deep);

        } finally {
            db.close();
        }

        return resultList.get(0);
    }

    private void updateObjToDb(Object obj, SQLiteDatabase db, String[] fields) {
        Class<?> cls = obj.getClass();
        HashSet<String> fieldsSet = new HashSet<>();

        if (fields != null)
            Collections.addAll(fieldsSet, fields);

        ClassItem classItem = classItems.get(cls);

        if (classItem != null) {
            String tableName = classItem.tableName;
            StringBuilder sql = new StringBuilder();
            sql.append("UPDATE ").append(tableName).append(" SET ");
            StringBuilder wherePart = new StringBuilder();

            for (Field field : cls.getDeclaredFields()) {
                Column column = field.getAnnotation(Column.class);
                field.setAccessible(true);

                if (column != null) {
                    String cName = column.columnName();
                    cName = cName.equals("") ? field.getName() : cName;
                    Object value;

                    try {
                        value = field.get(obj);

                        if (column.isPrimaryKey()) {
                            wherePart.append(cName).append("=");

                            if (value instanceof String) {
                                wherePart.append("\"").append(value).append("\"");

                            } else {
                                wherePart.append(value);
                            }

                        } else {
                            if (fields != null) {
                                if (!fieldsSet.contains(cName)) {
                                    continue;
                                }
                            }

                            sql.append(cName).append("=");
                            if (value instanceof String || value.getClass().isEnum()) {
                                sql.append("\"").append(value).append("\",");

                            } else if (value instanceof Boolean) {
                                sql.append(value.equals(Boolean.TRUE) ? 1 : 0);
                                sql.append(",");

                            } else {
                                sql.append(value).append(",");
                            }
                        }

                    } catch (IllegalArgumentException e) {
                        NLog.e(TAG, "", e);

                    } catch (IllegalAccessException e) {
                        NLog.e(TAG, "", e);
                    }
                }

                ObjectFromOtherTable objectFromOtherTable = field.getAnnotation(ObjectFromOtherTable.class);

                if (objectFromOtherTable != null) {
                    Type fieldClass = field.getType();

                    if (fields != null) {
                        if (!fieldsSet.contains(field.getName())) {
                            continue;
                        }
                    }

                    try {
                        if (Collection.class.isAssignableFrom((Class<?>) fieldClass)) {
                            Class<?> classType = getCollectionGenericClass(field);

                            if (classType != null) {
                                Collection<?> objs = (Collection<?>) field.get(obj);

                                if (objs != null) {
                                    for (Object o : objs) {
                                        updateObjToDb(o, db, null);
                                    }
                                }
                            }
                        } else {
                            updateObjToDb(field.get(obj), db, null);
                        }

                    } catch (IllegalArgumentException e) {
                        NLog.e(TAG, "", e);

                    } catch (IllegalAccessException e) {
                        NLog.e(TAG, "", e);
                    }
                }
            }

            sql.delete(sql.length() - 1, sql.length());
            sql.append(" WHERE ").append(wherePart.toString()).append(";");
            NLog.i(TAG, "sql:" + sql.toString());
            db.execSQL(sql.toString());
        } else {
            throw new RuntimeException("Wrong obj class (class must have annotation Table)");
        }
    }

    private int countObjsInDb(Class<?> cls, SQLiteDatabase db, String where) {
        ClassItem classItem = classItems.get(cls);
        int count = -1;
        if (classItem != null) {
            String tableName = classItem.tableName;
            String query = "select count(*) from " + tableName + " " + where + ";";
            Cursor c = null;
            try {
                c = db.rawQuery(query, null);
                c.moveToFirst();
                count = c.getInt(0);
            } finally {
                if (c != null) {
                    c.close();
                }
            }
            return count;
        } else {
            throw new RuntimeException("Wrong obj class (class must have annotation Table)");
        }
    }

    private void insertObjToDb(Object obj, SQLiteDatabase db, Pair<String, Long> idMapping) {
        Class<?> cls = obj.getClass();
        ClassItem classItem = classItems.get(cls);

        if (classItem != null) {
            String tableName = classItem.tableName;
            ContentValues cv = new ContentValues();
            ArrayList<Pair<ObjectFromOtherTable, Field>> foregeinFields = new ArrayList<>();
            Field primaryKeyField = null;

            for (int i = 0; i < classItem.fields.length; i++) {
                Field field = classItem.fields[i];
                Column column = classItem.columns[i];

                if (column != null) {
                    if (!column.isPrimaryKey()) {
                        String cName = classItem.names[i];
                        Object value;
                        try {
                            value = field.get(obj);
                            Class<?> valueCls = value.getClass();
                            if (idMapping != null && idMapping.first.equals(cName)) {
                                value = idMapping.second;
                                field.setLong(obj, idMapping.second);
                            }
                            if (value instanceof String || value.getClass().isEnum()) {
                                cv.put(cName, (String) value);

                            } else if (valueCls == Boolean.class || valueCls == boolean.class) {
                                cv.put(cName, value.equals(Boolean.TRUE) ? 1 : 0);

                            } else if (valueCls == Integer.class || valueCls == int.class) {
                                cv.put(cName, (Integer) value);

                            } else if (valueCls == Long.class || valueCls == long.class) {
                                cv.put(cName, (Long) value);

                            } else if (valueCls == Byte.class || valueCls == byte.class) {
                                cv.put(cName, (Byte) value);
                            } else {
                                throw new RuntimeException(value.getClass() + " is not supported datatype");
                            }
                        } catch (Exception e) {
                            NLog.e(TAG, "", e);
                        }
                    } else {
                        primaryKeyField = field;
                    }
                }

                ObjectFromOtherTable objectFromOtherTable = field.getAnnotation(ObjectFromOtherTable.class);
                if (objectFromOtherTable != null) {
                    foregeinFields.add(new Pair<>(objectFromOtherTable, field));
                }
            }

            db.insert(tableName, null, cv);
            Cursor c = db.rawQuery("Select last_insert_rowid();", null);
            c.moveToFirst();
            long lastId = c.getLong(0);
            c.close();

            if (primaryKeyField != null) {
                try {
                    primaryKeyField.set(obj, lastId);
                } catch (Exception e) {
                    NLog.e(TAG, "", e);
                }
            }
            for (Pair<ObjectFromOtherTable, Field> item : foregeinFields) {
                ObjectFromOtherTable objectFromOtherTable = item.first;
                Field field = item.second;
                if (objectFromOtherTable != null) {
                    Type fieldClass = field.getType();
                    try {
                        if (Collection.class.isAssignableFrom((Class<?>) fieldClass)) {
                            Pair<String, Long> idMap = new Pair<>(objectFromOtherTable.columnName(), lastId);
                            Class<?> classType = getCollectionGenericClass(field);
                            if (classType != null) {
                                Collection<?> objs = (Collection<?>) field.get(obj);
                                if (objs != null) {
                                    for (Object o : objs) {
                                        insertObjToDb(o, db, idMap);
                                    }
                                }
                            }
                        } else {
                            insertObjToDb(field.get(obj));
                        }
                    } catch (Exception e) {
                        NLog.e(TAG, "", e);
                    }
                }
            }
        } else {
            throw new RuntimeException("Wrong obj class (class must have annotation Table)");
        }
    }

    public void deleteObjsFromDb(Class<?> cls, String where) {
        ClassItem classItem = classItems.get(cls);
        if (classItem != null) {
            String tableName = classItem.tableName;
            StringBuilder sql = new StringBuilder();
            sql.append("DELETE FROM ").append(tableName).append(" ").append(where).append(";");
            NLog.i(TAG, "sql:" + sql.toString());
            SQLiteDatabase db = getWritableDatabase();
            try {
                db.execSQL(sql.toString());
            } finally {
                db.close();
            }
        } else {
            throw new RuntimeException("Wrong obj class (class must have annotation Table)");
        }
    }

    public void deleteObjFromDb(Object obj) {
        Class<?> cls = obj.getClass();
        ClassItem classItem = classItems.get(cls);

        if (classItem != null) {
            String tableName = classItem.tableName;
            StringBuilder sql = new StringBuilder();
            sql.append("DELETE FROM ").append(tableName).append(" WHERE ");
            for (Field field : cls.getDeclaredFields()) {
                Column column = field.getAnnotation(Column.class);
                field.setAccessible(true);
                if (column != null && column.isPrimaryKey()) {
                    String cName = column.columnName();
                    cName = cName.equals("") ? field.getName() : cName;
                    sql.append(cName).append("=");
                    Object value;
                    try {
                        value = field.get(obj);
                        if (value instanceof String) {
                            sql.append("\"").append(value).append("\"");
                        } else {
                            sql.append(value);
                        }
                    } catch (Exception e) {
                        NLog.e(TAG, "", e);
                    }
                }
            }
            sql.append(";");
            NLog.i(TAG, "sql:" + sql.toString());
            SQLiteDatabase db = getWritableDatabase();
            try {
                db.execSQL(sql.toString());
            } finally {
                db.close();
            }
        } else {
            throw new RuntimeException("Wrong obj class (class must have annotation Table)");
        }
    }

    private <T> ArrayList<T> selectObjsFromDb(Class<T> cls, SQLiteDatabase db, String where,
                                              String groupby, String orderBy, boolean deep) {
        ClassItem classItem = classItems.get(cls);
        if (classItem != null) {
            ArrayList<T> result = new ArrayList<>();
            String tableName = classItem.tableName;
            StringBuilder sql = new StringBuilder();
            long time = System.currentTimeMillis();
            sql.append("SELECT * FROM ").append(tableName);
            if (where != null)
                sql.append(" ").append(where);
            if (groupby != null)
                sql.append(" ").append(groupby);
            if (orderBy != null)
                sql.append(" ").append(orderBy);
            sql.append(";");
            Cursor cursor = db.rawQuery(sql.toString(), null);

            while (cursor.moveToNext()) {
                try {
                    T obj = cls.newInstance();
                    int i = 0;
                    String id = null;
                    ArrayList<Pair<ObjectFromOtherTable, Field>> objsFromOtherTable = null;
                    if (deep) {
                        objsFromOtherTable = new ArrayList<>();
                    }

                    for (int index = 0; index < classItem.fields.length; index++) {
                        Field field = classItem.fields[index];
                        Column column = classItem.columns[index];

                        if (column != null) {
                            Class<?> cl = classItem.classes[index];
                            if (cl == String.class) {
                                field.set(obj, cursor.getString(i));

                                if (index == classItem.primaryKeyIdx)
                                    id = "\"" + cursor.getString(i) + "\"";

                            } else if (cl.isEnum()) {
                                @SuppressWarnings({"unchecked", "rawtypes"})
                                Enum enu = Enum.valueOf((Class<Enum>) cl, cursor.getString(i));
                                field.set(obj, enu);

                                if (index == classItem.primaryKeyIdx)
                                    id = "\"" + cursor.getString(i) + "\"";

                            } else if (cl == int.class || cl == Integer.class) {
                                field.set(obj, cursor.getInt(i));
                                if (index == classItem.primaryKeyIdx)
                                    id = cursor.getInt(i) + "";

                            } else if (cl == long.class || cl == Long.class) {
                                field.set(obj, cursor.getLong(i));
                                if (index == classItem.primaryKeyIdx)
                                    id = cursor.getLong(i) + "";

                            } else if (cl == boolean.class || cl == Boolean.class) {
                                field.set(obj, cursor.getInt(i) == 1);

                            } else if (cl == float.class || cl == Float.class) {
                                field.set(obj, cursor.getFloat(i));
                            }
                            i++;
                        }

                        if (deep) {
                            ObjectFromOtherTable objectFromOtherTable = classItem.objsFromObjectFromOtherTable[index];
                            if (objectFromOtherTable != null) {
                                objsFromOtherTable.add(new Pair<>(objectFromOtherTable, field));
                            }
                        }
                    }
                    if (deep) {
                        for (Pair<ObjectFromOtherTable, Field> item : objsFromOtherTable) {
                            Type fieldClass = item.second.getType();
                            try {
                                if (Collection.class.isAssignableFrom((Class<?>) fieldClass)) {
                                    Class<?> classType = getCollectionGenericClass(item.second);
                                    if (classType != null) {
                                        String whereS = "WHERE " + item.first.columnName() + "=" + id;
                                        Collection<?> items = selectObjsFromDb(classType, db,
                                                whereS, groupby, orderBy, deep);
                                        item.second.set(obj, items);
                                    }
                                } else {
                                    NLog.e(TAG, "Not Implemented yet");
                                }
                            } catch (Exception e) {
                                NLog.e(TAG, "", e);
                            }
                        }
                    }
                    result.add(obj);
                } catch (Exception e) {
                    NLog.e(TAG, "", e);
                }
            }
            cursor.close();
            NLog.i(TAG, "total time:" + (System.currentTimeMillis() - time));
            return result;
        } else {
            throw new RuntimeException("Wrong obj class (class must have annotation Table)");
        }
    }

    private class ClassItem {
        Field[] fields = null;
        Column[] columns = null;
        ObjectFromOtherTable[] objsFromObjectFromOtherTable;
        Table table = null;
        String tableName = "";
        Class<?>[] classes = null;
        String[] names = null;
        int primaryKeyIdx = -1;

        public ClassItem(Class<?> cls) {
            fields = cls.getDeclaredFields();
            columns = new Column[fields.length];
            objsFromObjectFromOtherTable = new ObjectFromOtherTable[fields.length];
            classes = new Class<?>[fields.length];
            names = new String[fields.length];
            table = cls.getAnnotation(Table.class);
            tableName = table.tableName();
            tableName = tableName.equals("") ? cls.getSimpleName() : tableName;

            for (int i = 0; i < fields.length; i++) {
                Field f = fields[i];
                f.setAccessible(true);
                Column column = columns[i] = f.getAnnotation(Column.class);
                if (column != null) {
                    classes[i] = f.getType();
                    names[i] = column.columnName();
                    names[i] = names[i].equals("") ? f.getName() : names[i];
                    if (column.isPrimaryKey()) {
                        primaryKeyIdx = i;
                    }
                }
                ObjectFromOtherTable objectFromOtherTable = f.getAnnotation(ObjectFromOtherTable.class);
                if (objectFromOtherTable != null) {
                    objsFromObjectFromOtherTable[i] = objectFromOtherTable;
                }
            }
        }
    }
}

package nostalgia.framework.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据库列映射注解。
 * <p>标注在实体类字段上，用于 {@link nostalgia.framework.utils.DatabaseHelper}
 * 将 Java 字段映射为 SQLite 表列，支持主键、非空、唯一等约束。</p>
 *
 * @author NostalgiaLite
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface Column {
    /** 列名，为空时使用字段名 */
    String columnName() default "";

    /** 是否为主键 */
    boolean isPrimaryKey() default false;

    /** 是否允许为空值 */
    boolean allowNull() default true;

    /** 是否唯一约束 */
    boolean unique() default false;

    /** 是否创建索引 */
    boolean hasIndex() default false;
}

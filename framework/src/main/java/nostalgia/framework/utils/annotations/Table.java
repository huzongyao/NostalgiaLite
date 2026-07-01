package nostalgia.framework.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 数据库表映射注解。
 * <p>标注在实体类上，用于 {@link nostalgia.framework.utils.DatabaseHelper}
 * 将 Java 类映射为 SQLite 表。</p>
 *
 * @author NostalgiaLite
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Table {
    /** 表名，为空时使用类名 */
    String tableName() default "";
}

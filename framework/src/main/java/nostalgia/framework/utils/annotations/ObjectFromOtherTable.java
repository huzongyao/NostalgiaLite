package nostalgia.framework.utils.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 跨表关联注解。
 * <p>标注在实体类字段上，表示该字段引用另一张表的数据。
 * {@link nostalgia.framework.utils.DatabaseHelper} 在深度查询时
 * 会根据此注解自动加载关联表的记录。</p>
 *
 * @author NostalgiaLite
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Inherited
public @interface ObjectFromOtherTable {
    /** 关联表中用于匹配的外键列名 */
    String columnName();

}

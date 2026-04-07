package tech.kayys.gollek.agent.spi;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SkillDescriptor {
    String id() default "";

    String name();

    String description();

    String version() default "1.0.0";

    SkillCategory category() default SkillCategory.GENERAL;

    int priority() default 100;

    Input[] inputs() default {};

    Output[] outputs() default {};

    String[] triggers() default {};

    String[] aliases() default {};

    @interface Input {
        String name();

        String type() default "string";

        boolean required() default true;

        String description() default "";
    }

    @interface Output {
        String name();

        String type() default "string";

        String description() default "";
    }
}

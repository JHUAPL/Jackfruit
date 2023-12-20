package jackfruit.demo;

import jackfruit.annotations.Comment;
import jackfruit.annotations.DefaultValue;
import jackfruit.annotations.Jackfruit;

@Jackfruit(prefix = "included")
public interface Included {

    @DefaultValue("1")
    int includedIntMethod();

    @DefaultValue("1.5")
    double includedDoubleMethod();

}

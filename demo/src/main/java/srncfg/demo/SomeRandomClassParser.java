package srncfg.demo;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import srncfg.annotations.Parser;

public class SomeRandomClassParser implements Parser<SomeRandomClass> {

  public static Logger logger = LogManager.getLogger();

  @Override
  public SomeRandomClass fromString(String s) {
    logger.info("deserialize SomeRandomClass from " + s);
    return new SomeRandomClass();
  }

  @Override
  public String toString(SomeRandomClass src) {
    return "";
  }


}

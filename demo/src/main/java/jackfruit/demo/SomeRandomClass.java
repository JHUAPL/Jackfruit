package jackfruit.demo;

public class SomeRandomClass {

  private String internalString;

  public String getInternalString() {
    return internalString;
  }

  public SomeRandomClass(String internalString) {
    this.internalString = internalString;
  }

  public String toUpperCase() {
    return internalString.toUpperCase();
  }

}

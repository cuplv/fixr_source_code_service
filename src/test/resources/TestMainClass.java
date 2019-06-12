package simple;

public class TestMainClass {

  public static void main(String[] args) {

  }

  public int intMethod(String methodPar1) {
    InnerClass c = new InnerClass();

    InnerClass c1 = new InnerClass() {
      @Override
      public void innerClassMethod() {
        int i = 0;
        i = i + 1;
        return;
      }
    };

    InnerClass c2 = new InnerClass() {
      @Override
      public void innerClassMethod() {
        int i = 0;
        i = i + 1;
        return;
      }
    };

    InnerClass2 c3 = new InnerClass2() {
      @Override
      public void innerClass2Method() {
        int i = 0;
        i = i + 1;
        return;
      }
    };

    return 1;
  }

  class InnerClass {
    public void innerClassMethod() {
    }
  }

  class InnerClass2 {
    public InnerClass2() {
    }

    public void innerClass2Method() {
    }
  }

  public void callMethodA() {
    otherMethodToCall(3);
    otherMethodToCall(4);
  }

  public int otherMethodToCall(int i) {
    return i + 2;
  }

}

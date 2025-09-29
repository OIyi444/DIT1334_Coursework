package chapter1;
public class ex_OverLoading {
    String name;
    int age;
    // Constructor with parameters
    public ex_OverLoading(String name, int age) {
        this.name = name;
        this.age = age;
    }
    // Overloaded method 1: no parameters
    void sayHello() {
        System.out.println("Hello guys!");
    }
    
    // Overloaded method 2: one String parameter
    void sayHello(String name) {
        System.out.println("Hello guys, my name is " + name + "!");
    }
    // Overloaded method 3: one int parameter
    void sayHello(int age) {
        System.out.println("Hello guys, my name is " + name + ", I'm " + age + " years old!");
    }
    public static void main(String[] args) {
        // Create an object of ex_OverLoading using the constructor
        ex_OverLoading hello = new ex_OverLoading("Chong Yi", 20);
        System.out.println("--- Method Overloading Demo ---");
        // Call the overloaded methods
        hello.sayHello();             // Calls method with no parameters
        hello.sayHello("Chong Yi");   // Calls method with String parameter
        hello.sayHello(20);           // Calls method with int parameter
    }
}

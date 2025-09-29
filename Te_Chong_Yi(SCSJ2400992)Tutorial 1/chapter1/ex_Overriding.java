package chapter1;
// Parent class
class Animal {
    void makeSound() {
        System.out.println("The animal makes a sound");
    }
 }
// Child class (overrides the parent method)
class Dog extends Animal {
    @Override
    void makeSound() {
        System.out.println("The dog barks: Woof Woof!");
    }
 }
// Another Child class (also overrides the parent method)
class Cat extends Animal {
    @Override
    void makeSound() {
        System.out.println("The cat meows: Meow Meow!");
    }
 }
public class ex_Overriding {
    public static void main(String[] args) {
        // Create objects using parent reference
        Animal a1 = new Animal(); // Parent class object
        Animal a2 = new Dog();    // Dog object, but referenced as Animal
        Animal a3 = new Cat();    // Cat object, but referenced as Animal
        System.out.println("--- Method Overriding Demo ---");
        // Calls methods (runtime polymorphism decides which one to run)
        a1.makeSound();   // Calls Animal's method
        a2.makeSound();   // Calls Dog's overridden method
        a3.makeSound();   // Calls Cat's overridden method
    }
}

package chapter1;

// Parent class
class AnimalBase {
    void makeSound() {
        System.out.println("Animal makes a sound");
    }
}

// Child class: Reptile
class Reptile extends AnimalBase {
    void eat() {
        System.out.println("Reptile is eating food");
    }
}

// Child class: Mammal
class Mammal extends AnimalBase {
    void sleep() {
        System.out.println("Mammal is sleeping");
    }
}

// Child class: Insect
class Insect extends AnimalBase {
    void fly() {
        System.out.println("Insect is flying");
    }
}

public class ExInheritance {
    public static void main(String[] args) {
        // Create objects of parent and child classes
        AnimalBase a1 = new AnimalBase();
        Reptile a2 = new Reptile();
        Mammal a3 = new Mammal();
        Insect a4 = new Insect();

        System.out.println("--- Inheritance Demo ---");

        // Parent class method
        a1.makeSound();

        // Reptile inherits makeSound() from AnimalBase
        a2.makeSound();
        a2.eat();  // Reptile’s own method

        // Mammal inherits makeSound() from AnimalBase
        a3.makeSound();
        a3.sleep();  // Mammal’s own method

        // Insect inherits makeSound() from AnimalBase
        a4.makeSound();
        a4.fly();  // Insect’s own method
    }
}

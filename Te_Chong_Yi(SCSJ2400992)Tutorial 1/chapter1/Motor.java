package chapter1;
public class Motor {
    // Attributes (fields) of the Motor class
    String brand;   // Brand of the motor
    String plate;   // Plate number
    String owner;   // Owner's name

    // Constructor: initializes a new Motor object with brand, plate, and owner
    public Motor(String brand, String plate, String owner) {
        this.brand = brand;
        this.plate = plate;
        this.owner = owner;
    }
    // Method to display motor details
    public void detailInfo() {
        System.out.println("Motor brand: " + brand +
                           " plate is: " + plate +
                           " Owner is: " + owner);
    }
    // Main method: entry point of the program
    public static void main(String[] args) {
        // Create two Motor objects with different attributes
        Motor motor1 = new Motor("Suzuki", "PLL2224", "Te Chong Yi");
        Motor motor2 = new Motor("Yamaha", "ABC123", "Alex");

        // Call the detailInfo() method to display their details
        motor1.detailInfo();
        motor2.detailInfo();
    }
}

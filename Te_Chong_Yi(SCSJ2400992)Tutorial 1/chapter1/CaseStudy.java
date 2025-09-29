package chapter1;

class User{
	String username;
	
	void login() {
        System.out.println(username + " logged in.");
    }

    void logout() {
        System.out.println(username + " logged out.");
    }
}

class Customer extends User {
    void placeOrder() {
        System.out.println(username + " placed an order.");
    }
}

// Derived class: Admin
class Admin extends User {
    void manageProducts() {
        System.out.println(username + " is managing products.");
    }
}

//Derived class: Vendor
class Vendor extends User {
 void uploadProduct() {
     System.out.println(username + " uploaded a new product.");
 }
}
public class CaseStudy {
	 public static void main(String[] args) {
	        Customer c = new Customer();
	        c.username = "Alice";
	        c.login();
	        c.placeOrder();
	        c.logout();

	        Admin a = new Admin();
	        a.username = "Bob";
	        a.login();
	        a.manageProducts();
	        a.logout();
	    }
}

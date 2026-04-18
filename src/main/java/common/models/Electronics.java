package common.models;

public class Electronics extends Entity implements DisplayDetails{
    
    public Electronics(String id,String type, String name){
        super(id,type, name);
    }
    public void displayProductin4(){
        System.out.println("ID: " + id + " | Name: " + name);
        details.getDetails();
    }
}

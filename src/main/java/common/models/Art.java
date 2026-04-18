package common.models;
public class Art extends Entity implements DisplayDetails{
    public Art(String id, String type, String name){
         super(id, type, name);

    }
    public void displayProductin4(){
        System.out.println("ID: " + id + " | Name: " + name);
        details.getDetails();
    }


}

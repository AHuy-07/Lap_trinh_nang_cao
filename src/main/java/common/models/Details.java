package common.models;
import java.util.ArrayList;

public class Details {
    ArrayList<String> arr = new ArrayList<>();

    void addSpecification(String spec){
        arr.add(spec);
    }
    void removeSpec(){
        arr.clear();
    }
    void getDetails(){
        System.out.println("More information:...");
        for(String spec: arr){
            System.out.println("-- "+spec);
        }
    }
}

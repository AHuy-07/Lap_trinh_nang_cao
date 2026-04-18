package common.models;
public class Entity {
    protected String id;
    protected String type;
    protected String name;
    protected Details details;

    public Entity(String id,String type, String name){
        this.id = id;
        this.type = type;
        this.name = name;
    }
    
    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }
    public String getType(){
        return type;
    }

    public void setDetails(Details details){
        this.details = details;
    }

}   

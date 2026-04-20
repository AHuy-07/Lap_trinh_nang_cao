package common.models;
public class ArtFactory extends ItemFactory{
    public Entity createProducts(String id, String type, String name){
    if(type == null){
        throw new IllegalArgumentException("Type cannot be null!");
        }
    else if(type.equals("Art")){
        return new Art(id, type, name);
        }
    else{throw new IllegalArgumentException("Unknown type detect: " + type);}
    }
}

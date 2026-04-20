package common.models;
public class ElecFactory extends ItemFactory{
    public Entity createProducts(String id, String type, String name){
    if(type == null){
        throw new IllegalArgumentException("Type cannot be null!");
        }
    else if(type.equals("Electronics")){
        return new Electronics(id, type, name);
        }
    else{throw new IllegalArgumentException("Unknown type detect: " + type);}
    }
}

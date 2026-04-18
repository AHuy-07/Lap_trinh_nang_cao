package common.models;
abstract class ItemFactory {
    abstract Entity createProducts(String id, String type, String name);
}

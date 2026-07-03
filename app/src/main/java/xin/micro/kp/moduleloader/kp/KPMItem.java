package xin.micro.kp.moduleloader.kp;


import java.util.Map;

public class KPMItem{
    public String path;//最重要
    public KPMItem(String path){
        this.path = path;
    }
    public String name;
    public String description;
    public String license;
    public String author;

    /**
     * 通过path补全数据
     */
    public void complete(){
        Map<String, String> info = KPMParser.extractKpmInfo(path);
        this.name =  info.get("name");;
        this.author =  info.get("author");
        this.description =  info.get("description");
        this.license =  info.get("license");
    }

}
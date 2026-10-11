package me.ancal.wmdroid.sdk;

/** Immutable command-menu entry. Children are copied at both API boundaries. */
public final class WmMenuItem {
    public final int id;
    public final String label;
    public final boolean enabled,checked,separator;
    private final WmMenuItem[] children;
    private WmMenuItem(int id,String label,boolean enabled,boolean checked,boolean separator,WmMenuItem[] children){
        if(label==null)throw new IllegalArgumentException("Null menu label");
        this.id=id;this.label=label;this.enabled=enabled;this.checked=checked;this.separator=separator;
        this.children=children==null?new WmMenuItem[0]:children.clone();
        for(WmMenuItem child:this.children)if(child==null)throw new IllegalArgumentException("Null submenu entry");
    }
    public static WmMenuItem command(int id,String label){return new WmMenuItem(id,label,true,false,false,null);}
    public static WmMenuItem checked(int id,String label,boolean checked){return new WmMenuItem(id,label,true,checked,false,null);}
    public static WmMenuItem submenu(String label,WmMenuItem... children){
        if(children==null||children.length==0)throw new IllegalArgumentException("Empty submenu");
        return new WmMenuItem(0,label,true,false,false,children);
    }
    public static WmMenuItem separator(){return new WmMenuItem(0,"",false,false,true,null);}
    public WmMenuItem withEnabled(boolean enabled){return new WmMenuItem(id,label,enabled,checked,separator,children);}
    public WmMenuItem[] children(){return children.clone();}
    public boolean hasChildren(){return children.length!=0;}
}

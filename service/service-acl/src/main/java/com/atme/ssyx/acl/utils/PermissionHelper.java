package com.atme.ssyx.acl.utils;

import com.atme.ssyx.model.acl.Permission;

import java.util.ArrayList;
import java.util.List;

public class PermissionHelper{

    public static List<Permission> buildPermission(List<Permission> allList){
        List<Permission> trees = new ArrayList<>();
        for (Permission p : allList) {
            if (p.getPid() == 0){
                p.setLevel(1);
                //调用方法从第一层开始找
                trees.add(findChildren(p,allList));
            }
        }
        return trees;
    }

    private static Permission findChildren(Permission p, List<Permission> allList) {
        p.setChildren(new ArrayList<>());
        //遍历alllist所有菜单的数据
        for (Permission it: allList) {
            if (p.getId().equals(it.getPid())){
                int level = p.getLevel() + 1;
                it.setLevel(level);
                //封装下一层数据
                p.getChildren().add(findChildren(it,allList));
            }
        }
        return p;
    }


}

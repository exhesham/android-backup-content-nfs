package com.apps.exhesham.autoftpsync.utils;

import com.apps.exhesham.autoftpsync.Rules;

import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by hesham on 1/28/2017.
 */

public class PathDetails {
    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    private int depth;

    public PathDetails(File f){
        this.file = f;
    }

    public PathDetails(File f, int depth) {
        this.file = f;
        setDepth(depth);
    }

    public String getFilename(){
        return file.getName();
    }
    public String getLastModifiedDelta(){
        String res = "";
        Date nowDate = new Date();
        Date lastModified = new Date(file.lastModified());
        long diffInMillies = nowDate.getTime() - lastModified.getTime();
        List<TimeUnit> units = new ArrayList<TimeUnit>(EnumSet.allOf(TimeUnit.class));
        Collections.reverse(units);
        Map<TimeUnit,Long> result = new LinkedHashMap<TimeUnit,Long>();
        long milliesRest = diffInMillies;
        for ( TimeUnit unit : units ) {
            long diff = unit.convert(milliesRest,TimeUnit.MILLISECONDS);
            long diffInMilliesForUnit = unit.toMillis(diff);
            milliesRest = milliesRest - diffInMilliesForUnit;
            result.put(unit,diff);
        }
        Long days =  result.get(TimeUnit.DAYS);
        if(days == 0){
            return result.get(TimeUnit.HOURS).toString() +'h';
        }
        if(days > 365){
            return  new Integer((int) (days/365)).toString() + 'y';
        }
        return  String.valueOf(days) + 'd';

    }

    private File file;
    public boolean isDirectory() {
        return file.isDirectory();
    }

    public String getFullpath() {
        return file.getAbsolutePath();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PathDetails)) return false;

        PathDetails that = (PathDetails) o;

        return file != null ? file.getAbsolutePath().equals(that.file.getAbsolutePath()) : that.file == null;

    }

    @Override
    public int hashCode() {
        return file != null ? file.hashCode() : 0;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public String genPathRelativeToDepth() {
        String filepath = FilenameUtils.getFullPathNoEndSeparator(getFullpath());

        if(!isDirectory()){
            String extension = FilenameUtils.getExtension(getFullpath());
            String folderName = new Rules().getExtensionFolder(extension);
            if(folderName != null){
                return folderName;
            }else{
                return null;
            }
        }
        String res = FilenameUtils.getBaseName(filepath);
        for(int i = 0;i < getDepth();i++){
            filepath = FilenameUtils.getFullPathNoEndSeparator(filepath);
            res = FilenameUtils.getBaseName(filepath) + "/" + res;
        }
        return res;
    }
}

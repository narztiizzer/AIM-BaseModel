package group.aim.framework.basemodel;

import com.codesnippets4all.json.parsers.JSONParser;
import com.codesnippets4all.json.parsers.JsonParserFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import group.aim.framework.annotation.IQueryObject;
import group.aim.framework.annotation.JSONVariable;
import group.aim.framework.basedatetime.BaseDateTime;
import group.aim.framework.basemodel.exception.NotYetImplementedException;
import group.aim.framework.basemodel.exception.UnsupportedClassException;
import group.aim.framework.basemodel.interfaces.IJSONVariable;
import group.aim.framework.enumeration.ColumnType;
import group.aim.framework.enumeration.NullableType;
import group.aim.framework.enumeration.UpdatePolicy;
import group.aim.framework.helper.PrimitiveHelper;
import group.aim.framework.helper.StringHelper;


/**
 * Created by ponlavitlarpeampaisarl on 2/3/15 AD.
 * Editor :
 * Nattapong Rattasamut
 * Nattapong Poomtong
 */

public abstract class BaseModel implements IJSONVariable {
    @JSONVariable
    @IQueryObject(
            name = "update_date",
            type = ColumnType.TEXT,
            nullAble = NullableType.NOT_NULL
    )
    public BaseDateTime update_date;

    @JSONVariable
    public long unique_id = -2;
    @JSONVariable
    public boolean active_flag = true;

    @IQueryObject(
            name = "_id",
            type = ColumnType.INTEGER,
            nullAble = NullableType.NOT_NULL
    )
    public long _id = -1;
    private boolean isObjectFetched;
    private boolean isObjectFetching;
    private boolean isObjectProcessing;


    public BaseModel() {
        this.isObjectFetched = false;
        this.isObjectFetching = false;
        this.isObjectProcessing = false;
    }

    public BaseModel(String json, UpdatePolicy policy) {
        this();
        this.updateFromJson(json, policy);
    }

    public BaseModel(Map<String, Object> json, UpdatePolicy policy) {
        this();
        this.updateFromInfo(json, policy);
    }

    public String forceStringNotNull(String st) {
        if (st == null)
            st = "";
        return forceStringNotNullReplaceWith(st.replace("\\", ""), ConfigurationBaseModel.DEFAULT_NULL);
    }

    public String forceStringNotNullReplaceWith(String st, String newTxt) {
        if (st == null || st.trim().trim().equalsIgnoreCase("") || st.trim().equalsIgnoreCase("null"))
            st = newTxt;
        return st.trim().replace("\\", "");
    }

    public long getObjectId() {
        return this._id;
    }

    protected BaseModel self() {
        return this;
    }

    @Override
    public void updateTimeStamp() {
        this.update_date = new BaseDateTime();
    }

    @Override
    public BaseDateTime getupdate_date() {
        return this.update_date == null ? new BaseDateTime() : this.update_date;
    }

    public boolean getIsObjectProcessing() {
        return this.isObjectProcessing;
    }

    public boolean getIsObjectFetching() {
        return this.isObjectFetching;
    }

    public boolean getIsObjectFetched() {
        return this.isObjectFetched;
    }

    @Override
    public String toJSONLocalString() {
        return this.toJSONObject(true).toString().trim();
    }

    @Override
    public String toJSONString() {
        return this.toJSONObject(false).toString().trim();
    }

    /**
     * @param forLocal determine either generate json for local only
     * @return JSONObject that represent the properties in class that define JSONVariable annotation.
     */
    public JSONObject toJSONObject(boolean forLocal) {
        JSONObject root = new JSONObject();
        Field[] allField = null;
        Field[] allChildField = getClass().getDeclaredFields();
        if (getClass().getSuperclass() != null) {
            Field[] allSuperField = getClass().getSuperclass().getDeclaredFields();
            allField = Arrays.copyOf(allChildField, allChildField.length + allSuperField.length);
            System.arraycopy(allSuperField, 0, allField, allChildField.length, allSuperField.length);
        } else {
            allField = allChildField;
        }
        for (Field aField : allField) {
            if (aField.isAnnotationPresent(JSONVariable.class)) {
                try {
                    aField.setAccessible(true);
                    String name = aField.getName();
                    Object value = aField.get(this);
                    JSONVariable aFieldAnnotation = aField.getAnnotation(JSONVariable.class);
                    // Check the caller is for local
                    if (!forLocal && aFieldAnnotation.localOnly()) {
                        continue;
                    }
                    if (value != null) {
                        if (Boolean.TYPE.isAssignableFrom(aField.getType()))
                            root.put(name, ((Boolean) value).booleanValue() ? "1" : "0");
                        else if (aField.getType().isPrimitive() || String.class.isAssignableFrom(value.getClass())) {
                            root.put(name, StringHelper.HTMLNumberToString(value.toString().trim()));
                        } else if (BaseDateTime.class.isAssignableFrom(value.getClass()))
                            root.put(name, ((BaseDateTime) value).toUnixTimeString());
                        else if (IJSONVariable.class.isAssignableFrom(value.getClass()))
                            root.put(name, ((IJSONVariable) value).toJSONObject(forLocal));
                        else if (aField.getType().equals(Object.class)) {
                            root.put(name, value.toString().trim());
                        } else if (aField.getType().equals(ArrayList.class)) {
                            JSONArray array = new JSONArray();
                            for (int i = 0; i < ((ArrayList) value).size(); i++) {
                                array.put(((ArrayList) value).get(i));
                            }
                            root.put(name, array);
                        } else if (aField.getType().equals(HashMap.class)) {
                            root.put(name, value.toString().trim());
                        } else throw new UnsupportedClassException();
                    }
                    aField.setAccessible(false);
                } catch (IllegalAccessException ex) {
                    ex.printStackTrace();
                } catch (JSONException ex) {
                    ex.printStackTrace();
                }
            }
        }
        return root;
    }

    private Field getField(Class classStruct, String fieldName) {
        try {
            return classStruct.getDeclaredField(fieldName);
        } catch (NoSuchFieldException e) {
            try {
                return classStruct.getSuperclass().getDeclaredField(fieldName);
            } catch (NoSuchFieldException ex) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * Use to replace the current data from input value Map to this object
     *
     * @param info   field value map
     * @param policy update policy
     */
    public void updateFromInfo(Map<String, Object> info, UpdatePolicy policy) {

        for (String key : info.keySet()) {
            try {
                Field aField = getField(getClass(), key);
                if (aField == null) continue;
                aField.setAccessible(true);
                Object value = aField.get(this);
                Object newValue = info.get(key);

                if (newValue != null && !newValue.equals("null"))
                    switch (policy) {
                        case ForceUpdate: {
                            if (aField.getType().equals(Boolean.TYPE))
                                aField.set(this, PrimitiveHelper.boolFromString(newValue.toString().trim()));
                            else if (aField.getType().isPrimitive()) {
                                String tmpValue = newValue.toString().trim();
                                if (tmpValue.equalsIgnoreCase("null")) tmpValue = "-1";
                                if (aField.getType() == Integer.TYPE)
                                    aField.set(this, Integer.parseInt(tmpValue));
                                else if (aField.getType() == Float.TYPE)
                                    aField.set(this, Float.parseFloat(tmpValue));
                                else if (aField.getType() == Double.TYPE)
                                    aField.set(this, Double.parseDouble(tmpValue));
                                else if (aField.getType() == Long.TYPE)
                                    aField.set(this, Long.parseLong(tmpValue));
                                else throw new UnsupportedClassException();
                            } else if (aField.getType().equals(HashMap.class)) {
                                aField.set(this, newValue);
                            } else if (aField.getType().equals(Object.class)) {
                                aField.set(this, newValue);
                            } else if (aField.getType().equals(ArrayList.class)) {
                                aField.set(this, newValue);
                            } else if (String.class.isAssignableFrom(aField.getType()))
                                aField.set(this, StringHelper.HTMLNumberToString(newValue.toString().trim()));
                            else if (BaseDateTime.class.isAssignableFrom(aField.getType())) {
                                long uTime = Long.parseLong(newValue.toString().trim().replace(".", ""));
                                if (newValue.toString().trim().length() > 10)
                                    uTime = Long.parseLong(newValue.toString().trim().substring(0, 10));
                                aField.set(this, new BaseDateTime(uTime));
                            } else if (IJSONVariable.class.isAssignableFrom(aField.getType())) {
                                if (BaseArrayList.class.isAssignableFrom(aField.getType())) {
                                    if (newValue != null && !newValue.toString().trim().equalsIgnoreCase("null")) {
                                        ((BaseArrayList) value).updateFromArray((List) newValue, policy);
                                        aField.set(this, value);
                                    }
                                } else if (BaseModel.class.isAssignableFrom(aField.getType())) {
                                    if (value == null)
                                        try {
                                            value = aField.getType().newInstance();
                                        } catch (InstantiationException e) {
                                            e.printStackTrace();
                                        }
                                    if (newValue != null && !newValue.toString().equalsIgnoreCase("null")) {
                                        ((BaseModel) value).updateFromInfo((Map<String, Object>) newValue, policy);
                                        aField.set(this, value);
                                    }
                                }
                            } else throw new UnsupportedClassException();
                        }
                        break;
                        case UseFromCacheIfAvailable: {
                            if (aField.getType().equals(Boolean.TYPE))
                                aField.set(this, PrimitiveHelper.boolFromString(newValue.toString().trim()));
                            else if (aField.getType().isPrimitive()) {
                                String tmpValue = newValue.toString().trim();
                                if (tmpValue.equalsIgnoreCase("null")) tmpValue = "-1";
                                if (aField.getType() == Integer.TYPE)
                                    aField.set(this, Integer.parseInt(tmpValue));
                                else if (aField.getType() == Float.TYPE)
                                    aField.set(this, Float.parseFloat(tmpValue));
                                else if (aField.getType() == Double.TYPE)
                                    aField.set(this, Double.parseDouble(tmpValue));
                                else if (aField.getType() == Long.TYPE)
                                    aField.set(this, Long.parseLong(tmpValue));
                                else throw new UnsupportedClassException();
                            } else if (aField.getType().equals(HashMap.class)) {
                                aField.set(this, newValue);
                            } else if (aField.getType().equals(Object.class)) {
                                aField.set(this, newValue);
                            } else if (aField.getType().equals(ArrayList.class)) {
                                aField.set(this, newValue);
                            } else if (String.class.isAssignableFrom(aField.getType()))
                                aField.set(this, StringHelper.HTMLNumberToString(newValue.toString().trim()));
                            else if (BaseDateTime.class.isAssignableFrom(aField.getType())) {
                                long uTime = Long.parseLong(newValue.toString().trim().replace(".", ""));
                                if (newValue.toString().trim().length() > 10)
                                    uTime = Long.parseLong(newValue.toString().trim().substring(0, 10));
                                aField.set(this, new BaseDateTime(uTime));
                            } else if (IJSONVariable.class.isAssignableFrom(aField.getType())) {
                                if (BaseArrayList.class.isAssignableFrom(aField.getType())) {
                                    if (newValue != null && !newValue.toString().trim().equalsIgnoreCase("null")) {
                                        ((BaseArrayList) value).updateFromArray((List) newValue, policy);
                                        aField.set(this, value);
                                    }
                                } else if (BaseModel.class.isAssignableFrom(aField.getType())) {
                                    if (value == null)
                                        try {
                                            value = aField.getType().newInstance();
                                        } catch (InstantiationException e) {
                                            e.printStackTrace();
                                        }
                                    ((BaseModel) value).updateFromInfo((Map<String, Object>) newValue, policy);
                                    aField.set(this, value);
                                }
                            } else throw new UnsupportedClassException();
                        }
                        break;
                        case UseNewest: {
                            if (aField.getType().equals(Boolean.TYPE))
                                aField.set(this, PrimitiveHelper.boolFromString(newValue.toString().trim()));
                            else if (aField.getType().isPrimitive()) {
                                String tmpValue = newValue.toString().trim();
                                if (tmpValue.equalsIgnoreCase("null")) tmpValue = "-1";
                                if (aField.getType() == Integer.TYPE)
                                    aField.set(this, Integer.parseInt(tmpValue));
                                else if (aField.getType() == Float.TYPE)
                                    aField.set(this, Float.parseFloat(tmpValue));
                                else if (aField.getType() == Double.TYPE)
                                    aField.set(this, Double.parseDouble(tmpValue));
                                else if (aField.getType() == Long.TYPE)
                                    aField.set(this, Long.parseLong(tmpValue));
                                else throw new UnsupportedClassException();
                            } else if (aField.getType().equals(HashMap.class)) {
                                aField.set(this, newValue);
                            } else if (aField.getType().equals(ArrayList.class)) {
                                aField.set(this, newValue);
                            } else if (aField.getType().equals(Object.class)) {
                                aField.set(this, newValue);
                            } else if (String.class.isAssignableFrom(aField.getType()))
                                aField.set(this, StringHelper.HTMLNumberToString(newValue.toString().trim()));
                            else if (BaseDateTime.class.isAssignableFrom(aField.getType())) {
                                long uTime = Long.parseLong(newValue.toString().trim().replace(".", ""));
                                if (newValue.toString().trim().length() > 10)
                                    uTime = Long.parseLong(newValue.toString().trim().substring(0, 10));
                                aField.set(this, new BaseDateTime(uTime));
                            } else if (IJSONVariable.class.isAssignableFrom(aField.getType())) {
                                if (BaseArrayList.class.isAssignableFrom(aField.getType())) {
                                    if (newValue != null && !newValue.toString().trim().equalsIgnoreCase("null")) {
                                        ((BaseArrayList) value).updateFromArray((List) newValue, policy);
                                        aField.set(this, value);
                                    }
                                } else if (BaseModel.class.isAssignableFrom(aField.getType())) {
                                    if (value == null)
                                        try {
                                            value = aField.getType().newInstance();
                                        } catch (InstantiationException e) {
                                            e.printStackTrace();
                                        }
                                    ((BaseModel) value).updateFromInfo((Map<String, Object>) newValue, policy);
                                    aField.set(this, value);
                                }
                            } else throw new UnsupportedClassException();
                        }
                        case MergeToNewest: {
                            throw new NotYetImplementedException();
                        }
                    }
                aField.setAccessible(false);
            } catch (IllegalAccessException ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Update value from json string to this object with policy option.
     *
     * @param json   The string represent the class.
     * @param policy
     */
    public void updateFromJson(String json, UpdatePolicy policy) {
        JSONParser parser = JsonParserFactory.getInstance().newJsonParser();
        Map<String, Object> jsonData = parser.parseJson(json);
        if (jsonData.containsKey(ConfigurationBaseModel.getInstance().getEntriesFieldName()))
            updateFromInfo((Map<String, Object>) jsonData.get(ConfigurationBaseModel.getInstance().getEntriesFieldName()), policy);
        else
            updateFromInfo(jsonData, policy);
    }
}

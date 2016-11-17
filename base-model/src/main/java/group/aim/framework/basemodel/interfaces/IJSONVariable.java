package group.aim.framework.basemodel.interfaces;

import group.aim.framework.basedatetime.BaseDateTime;

/**
 * Created by Nattapongr on 9/28/2016 AD.
 */

public interface IJSONVariable<T> {

    String toJSONLocalString();

    String toJSONString();

    Object toJSONObject(boolean isLocaleOnly);

    void updateTimeStamp();

    BaseDateTime getupdate_date();
}

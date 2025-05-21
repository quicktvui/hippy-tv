package com.quicktvui.hippyext.views.fastlist.diff;

import static com.quicktvui.hippyext.views.fastlist.diff.KeyDiffHelper.handleKeyDataDiff;

import android.text.TextUtils;

import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * tv-list 实现diff工具类
 */
public class FastListDataBindingHelper {

  public interface OnDataDiffListener2 {

    void onMove(int position, int nextPosition);

    void onRangeUpdate(int start, int count);

    void onAllChange();
  }

  public interface OnDataDiffListener3 extends OnDataDiffListener2 {

    void onRangeInsert(int position, int count);

    void onRangeDelete(int position, int count);

    void onUpdateViewData();
  }

  // 不需要转换旧数据，直接调用此方法
  public static void handleDataDiffCallBack3(HippyArray beforeArray, HippyArray array,
                                             String keyName, OnDataDiffListener3 listener) {
    handleDataDiffCallBack3(beforeArray, array, keyName, listener, null, null);
  }

  /**
   * 回调方式的diff方法
   *
   * @param beforeArray       旧数据
   * @param array             新数据
   * @param keyName           指定key的名字，String类型   数据没有key值的话，直接传空
   * @param resultListener    diff结果回调监听
   * @param transFormListener 处理旧数据item转换
   * @param tClass            旧数据的Class类型
   * @param <T>               旧数据的类型
   */
  public static <T> void handleDataDiffCallBack3(HippyArray beforeArray, HippyArray array,
                                                 String keyName, OnDataDiffListener3 resultListener,
                                                 OnTransFormListener<T> transFormListener, Class<T> tClass) {
    Map<Integer, List<DiffItem>> resultMap = handleDataDiff(beforeArray, array, keyName, transFormListener, tClass);

    if (resultListener == null) {
      return;
    }

    resultListener.onUpdateViewData();

    if (resultMap.get(Patch.ALL_UPDATE) != null) {
      resultListener.onAllChange();
      return;
    }

    List<DiffItem> upList = resultMap.get(Patch.TYPE_UPDATE);
    List<DiffItem> deList = resultMap.get(Patch.TYPE_DELETE);
    List<DiffItem> insertList = resultMap.get(Patch.TYPE_INSERT);

    if (!TextUtils.isEmpty(keyName)) {
      List<DiffItem> moList = resultMap.get(Patch.TYPE_MOVE);
      handleKeyCallBack(upList, deList, insertList, moList, resultListener, array.size());
    } else {
      handleNoKeyCallBack(upList, deList, insertList, resultListener);
    }
  }

  private static void handleKeyCallBack(List<DiffItem> upList, List<DiffItem> deList,
                                        List<DiffItem> insertList, List<DiffItem> moList,
                                        OnDataDiffListener3 resultListener, int newSize) {
    // update
    commonNotify(upList, Patch.TYPE_UPDATE, resultListener);
    int max = -1, min = -1;
    // insert
    if (insertList.size() > 0) {
      int inMin = commonNotify(insertList, Patch.TYPE_INSERT, resultListener);
      if (inMin != -1) {
        min = inMin;
        max = newSize - 1;
      }
    }
    // move
    if (moList.size() > 0) {
      for (int i = 0; i < moList.size(); i++) {
        if (moList.get(i) instanceof MoveDiffItem) {
          MoveDiffItem moveDiffItem =
            (MoveDiffItem) moList.get(i);
          resultListener.onMove(moveDiffItem.position, moveDiffItem.nextPosition);
          max = Math.max(max, Math.max(moveDiffItem.position, moveDiffItem.nextPosition));
          if (min == -1) {
            min = Math.min(moveDiffItem.position, moveDiffItem.nextPosition);
          } else {
            min = Math.min(min, Math.min(moveDiffItem.position, moveDiffItem.nextPosition));
          }
        }
      }
    }
    // delete
    if (deList.size() > 0) {

      for (int i = deList.size() - 1; i >= 0; i--) {
        RangeDiffItem item = (RangeDiffItem) deList.get(i);
        resultListener.onRangeDelete(item.position, item.getListSize());
      }

      int deMin = deList.get(0).position;
      min = min == -1 ? deMin : Math.min(min, deMin);
      max = newSize - 1;
    }
    if (min != -1 && max != -1 && needRangUpdate(deList, insertList, moList, newSize)) {
      max = Math.min(max, newSize - 1);

      resultListener.onRangeUpdate(min, max - min + 1);
    }
  }

  private static boolean needRangUpdate(List<DiffItem> deList, List<DiffItem> insertList,
                                        List<DiffItem> moList, int newSize) {
    if (moList.size() != 0) {
      return true;
    }
    if (deList.size() == 1 && insertList.size() == 0) {
      RangeDiffItem item = (RangeDiffItem) deList.get(0);
      return (item.isReverse ? item.getListLast() : item.position) + item.getListSize() != newSize;
    }
    if (insertList.size() == 1 && deList.size() == 0) {
      RangeDiffItem item = (RangeDiffItem) insertList.get(0);
      return (item.isReverse ? item.getListLast() : item.position) + item.getListSize() != newSize;
    }
    return true;
  }

  private static void handleNoKeyCallBack(List<DiffItem> upList, List<DiffItem> deList,
                                          List<DiffItem> insertList, OnDataDiffListener3 resultListener) {
    // update
    commonNotify(upList, Patch.TYPE_UPDATE, resultListener);
    // delete
    commonNotify(deList, Patch.TYPE_DELETE, resultListener);
    // insert
    commonNotify(insertList, Patch.TYPE_INSERT, resultListener);
  }

  private static int commonNotify(List<DiffItem> diffItems, int type, OnDataDiffListener3 resultListener) {
    int min = -1;
    if (diffItems.size() > 0) {
      DiffItem item;
      for (int i = 0; i < diffItems.size(); i++) {
        item = diffItems.get(i);
        if (item instanceof RangeDiffItem) {
          int minPos = ((RangeDiffItem) item).notify(type, resultListener);
          min = min == -1 ? minPos : Math.min(minPos, min);
        }
      }
    }
    return min;
  }

  // 不需要转换旧数据，直接调用此方法
  public static Map<Integer, List<DiffItem>> handleDataDiff(HippyArray beforeArray, HippyArray array,
                                                            String keyName) {
    return handleDataDiff(beforeArray, array, keyName, null, null);
  }

  /**
   * 无回调形式的diff方法，结果直接返回
   *
   * @param beforeArray 旧数据
   * @param array       新数据
   * @param keyName     指定key的名字，String类型   数据没有key值的话，直接传空
   * @param listener    处理旧数据item转换
   * @param tClass      旧数据的Class类型
   * @param <T>         旧数据的类型
   * @return 包含list结果的Map
   */
  public static <T> Map<Integer, List<DiffItem>> handleDataDiff(HippyArray beforeArray, HippyArray array,
                                                                String keyName,
                                                                OnTransFormListener<T> listener, Class<T> tClass) {
    if (beforeArray == null || array == null || beforeArray.size() < 1 || array.size() < 1) {
      Map<Integer, List<DiffItem>> resultMap = new HashMap<>();
      resultMap.put(Patch.ALL_UPDATE, new ArrayList<>());
      return resultMap;
    }
    if (!TextUtils.isEmpty(keyName)) {
      return handleKeyDataDiff(beforeArray, array, keyName, listener, tClass);
    } else {
      return NoKeyDiffHelper.handleNoKeyDataDiff(beforeArray, array, listener, tClass);
    }
  }

  public static class MoveDiffItem extends DiffItem {
    public final int nextPosition;
    public final Object nextItem;

    public MoveDiffItem(int position, int nextPosition, Object item, Object nextItem) {
      super(position, item);
      this.nextPosition = nextPosition;
      this.nextItem = nextItem;
    }
  }

  public static class RangeDiffItem extends DiffItem {

    protected final List<Integer> posList = new ArrayList<>();

    protected final boolean isReverse;

    public static void handleRangeList(List<DiffItem> rangeList,
                                       int position, Object item) {
      handleRangeList(rangeList, position, item, false);
    }

    public static void handleRangeList(List<DiffItem> rangeList,
                                       int position, Object item, boolean isReverse) {

      if (rangeList.size() < 1) {
        rangeList.add(RangeDiffItem.getInstance(null, position, item, isReverse));
      } else {
        // 获取最近添加的数据
        RangeDiffItem diffItem = (RangeDiffItem) rangeList.get(rangeList.size() - 1);

        RangeDiffItem instance = RangeDiffItem.getInstance(diffItem, position, item, isReverse);
        if (instance != null) {
          rangeList.add(instance);
        }
      }
    }

    public static RangeDiffItem getInstance(RangeDiffItem diffItem, int nowPosition,
                                            Object nowItem, boolean isReverse) {
      if (diffItem != null && nowPosition == diffItem.getListLast() + (isReverse ? -1 : 1)) {
        diffItem.addPosition(nowPosition);
        return null;
      } else {
        return new RangeDiffItem(nowPosition, nowItem, isReverse);
      }
    }

    public RangeDiffItem(int position, Object item, boolean isReverse) {
      super(position, item);
      this.isReverse = isReverse;
      posList.add(position);
    }

    public int notify(int type, OnDataDiffListener3 listener) {
      int pos = isReverse ? getListLast() : position;
      switch (type) {
        case Patch.TYPE_UPDATE:
          listener.onRangeUpdate(pos, getListSize());
          break;
        case Patch.TYPE_INSERT:
          listener.onRangeInsert(pos, getListSize());
          break;
        case Patch.TYPE_DELETE:
          listener.onRangeDelete(pos, getListSize());
          break;
      }
      return pos;
    }

    public int getListSize() {
      return posList.size();
    }

    private void addPosition(int nowPosition) {
      posList.add(nowPosition);
    }

    private Integer getListLast() {
      return posList.get(posList.size() - 1);
    }
  }

  public static class DiffItem {

    public final int position;
    public final Object item;

    public DiffItem(int position, Object item) {
      this.position = position;
      this.item = item;
    }

    @Override
    public String toString() {
      return "DiffItem{" +
        "position=" + position +
        ", item=" + item +
        '}';
    }
  }

  public static class Patch {

    public static final int TYPE_UPDATE = 0; // 更新
    public static final int TYPE_DELETE = 1; // 删除
    public static final int TYPE_INSERT = 2; // 插入
    public static final int TYPE_MOVE = 3; // 移动 目前移动的情况，没有判断该数据有没有update
    public static final int ALL_UPDATE = 4; // 通知全部更新
  }

  public interface OnTransFormListener<T> {
    HippyMap onTransForm(T item);
  }
}

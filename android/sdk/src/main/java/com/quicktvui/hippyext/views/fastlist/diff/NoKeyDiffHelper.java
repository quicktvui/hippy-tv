package com.quicktvui.hippyext.views.fastlist.diff;

import static com.quicktvui.hippyext.views.fastlist.diff.FastListDataBindingHelper.RangeDiffItem.handleRangeList;

import android.text.TextUtils;

import com.quicktvui.hippyext.views.fastlist.diff.FastListDataBindingHelper.DiffItem;
import com.quicktvui.hippyext.views.fastlist.diff.FastListDataBindingHelper.OnTransFormListener;
import com.quicktvui.hippyext.views.fastlist.diff.FastListDataBindingHelper.Patch;
import com.quicktvui.hippyext.views.fastlist.diff.FastListDataBindingHelper.RangeDiffItem;
import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 无key diff实现类
 */
public class NoKeyDiffHelper {

  /**
   * 无key diff入口方法
   *
   * @param beforeArray 旧数据
   * @param array       新数据
   * @param listener    旧数据item转换
   * @param tClass      旧数据item Class类型
   * @param <T>         旧数据item类型
   * @return diff结果map
   */
  public static <T> Map<Integer, List<DiffItem>> handleNoKeyDataDiff(HippyArray beforeArray, HippyArray array,
                                                                     OnTransFormListener<T> listener, Class<T> tClass) {

    Map<Integer, List<DiffItem>> resultMap = new HashMap<>();
    List<DiffItem> updateList = new ArrayList<>();
    List<DiffItem> insertList = new ArrayList<>();
    List<DiffItem> deleteList = new ArrayList<>();
    resultMap.put(Patch.TYPE_UPDATE, updateList);
    resultMap.put(Patch.TYPE_INSERT, insertList);
    resultMap.put(Patch.TYPE_DELETE, deleteList);

    diffArray(beforeArray, array, resultMap, listener, tClass);

    return resultMap;
  }

  private static <T> void diffArray(HippyArray oldArray, HippyArray newArray,
                                    Map<Integer, List<DiffItem>> resultMap,
                                    OnTransFormListener<T> listener, Class<T> tClass) {
    diffArray(oldArray, newArray, resultMap, 0, -1, null, listener, tClass);
  }

  public static <T> void diffArray(HippyArray oldArray, HippyArray newArray,
                                   Map<Integer, List<DiffItem>> resultMap,
                                   int diffLevel, int rootPosition, Object rootItem,
                                   OnTransFormListener<T> listener, Class<T> tClass) {

    List<DiffItem> updateList = resultMap.get(Patch.TYPE_UPDATE);
    List<DiffItem> insertList = resultMap.get(Patch.TYPE_INSERT);
    List<DiffItem> deleteList = resultMap.get(Patch.TYPE_DELETE);

    assert updateList != null;
    assert insertList != null;
    assert deleteList != null;

    if (diffLevel != 0 && oldArray.size() != newArray.size()) {
      handleRangeList(updateList, rootPosition, rootItem);
      return;
    }

    for (int position = 0; position < oldArray.size(); position++) {
      Object oldItem = oldArray.getObject(position);
      if (diffLevel == 0 && listener != null && oldItem.getClass() == tClass) {
        oldItem = listener.onTransForm((T) oldItem);
      }
      Object newItem;
      if (position < newArray.size()) {
        newItem = newArray.getObject(position);
      } else { // 说明新数组比较短
        if (diffLevel != 0) {
          handleRangeList(updateList, rootPosition, rootItem);
          return;
        } else {
          handleRangeList(deleteList, position, oldItem);
          continue;
        }
      }
      if (oldItem instanceof Boolean) {
        boolean fromBool = (boolean) oldItem;
        if (!(newItem instanceof Boolean) || fromBool != (boolean) newItem) {
          if (diffLevel != 0) {
            handleRangeList(updateList, rootPosition, rootItem);
            return;
          } else {
            handleRangeList(updateList, position, newItem);
          }
        }
      } else if (oldItem instanceof Number) {
        boolean isSame = false;
        double fromDoubleValue = ((Number) oldItem).doubleValue();
        if (newItem instanceof Number) {
          double toDoubleValue = ((Number) newItem).doubleValue();
          isSame = (fromDoubleValue == toDoubleValue);
        }
        if (!isSame) {
          if (diffLevel != 0) {
            handleRangeList(updateList, rootPosition, rootItem);
            return;
          } else {
            handleRangeList(updateList, position, newItem);
          }
        }
      } else if (oldItem instanceof String) {
        if (newItem == null || !TextUtils.equals(oldItem.toString(), newItem.toString())) {
          if (diffLevel != 0) {
            handleRangeList(updateList, rootPosition, rootItem);
            return;
          } else {
            handleRangeList(updateList, position, newItem);
          }
        }
      } else if (oldItem instanceof HippyArray) {
        if (newItem instanceof HippyArray) {
          if (diffLevel != 0) {
            int sizeBefore = updateList.size();
            int innerSizeBefore = -1;
            RangeDiffItem item = null;
            if (sizeBefore > 0) {
              item = (RangeDiffItem) updateList.get(sizeBefore - 1);
              innerSizeBefore = item.getListSize();
            }
            diffArray((HippyArray) oldItem, (HippyArray) newItem, resultMap,
              diffLevel + 1, rootPosition, rootItem, listener, tClass);
            // update有新增，不需要继续循环
            if (sizeBefore != updateList.size() || innerSizeBefore != -1 && innerSizeBefore != item.getListSize()) {
              break;
            }
          } else {
            diffArray((HippyArray) oldItem, (HippyArray) newItem, resultMap,
              diffLevel + 1, position, newItem, listener, tClass);
          }
        } else {
          if (diffLevel != 0) {
            handleRangeList(updateList, rootPosition, rootItem);
            return;
          } else {
            handleRangeList(updateList, position, newItem);
          }
        }
      } else if (oldItem instanceof HippyMap) {
        if (newItem instanceof HippyMap) {
          if (diffLevel != 0) {
            int sizeBefore = updateList.size();
            int innerSizeBefore = -1;
            RangeDiffItem item = null;
            if (sizeBefore > 0) {
              item = (RangeDiffItem) updateList.get(sizeBefore - 1);
              innerSizeBefore = item.getListSize();
            }
            diffMap((HippyMap) oldItem, (HippyMap) newItem, resultMap,
              diffLevel + 1, rootPosition, rootItem, listener, tClass);
            // 这快应该判断是否进入继续循环
            if (sizeBefore != updateList.size() || innerSizeBefore != -1 && innerSizeBefore != item.getListSize()) {
              break;
            }
          } else {
            diffMap((HippyMap) oldItem, (HippyMap) newItem, resultMap,
              diffLevel + 1, position, newItem, listener, tClass);
          }
        } else {
          if (diffLevel != 0) {
            handleRangeList(updateList, rootPosition, rootItem);
            return;
          } else {
            handleRangeList(updateList, position, newItem);
          }
        }
      }
    }

    if (newArray.size() > oldArray.size()) {
      for (int position = oldArray.size(); position < newArray.size(); position++) {
        if (diffLevel != 0) {
          handleRangeList(updateList, rootPosition, rootItem);
        } else {
          handleRangeList(insertList, position, newArray.get(position));
        }
      }
    }
  }

  protected static <T> void diffMap(HippyMap oldMap, HippyMap newMap, Map<Integer,
    List<DiffItem>> resultMap, int diffLevel, int rootPosition, Object rootItem,
                                    OnTransFormListener<T> listener, Class<T> tClass) {
    assert resultMap != null;

    List<DiffItem> updateList = resultMap.get(Patch.TYPE_UPDATE);

    assert updateList != null;

    if (oldMap.keySet().size() != newMap.keySet().size()) {
      handleRangeList(updateList, rootPosition, rootItem);
      return;
    }

    for (String key : oldMap.keySet()) {
      Object oldValue = oldMap.get(key);
      Object newValue = newMap.get(key);

      if (oldValue instanceof Boolean) {
        boolean fromBool = (boolean) oldValue;
        if (!(newValue instanceof Boolean) || fromBool != (boolean) newValue) {
          handleRangeList(updateList, rootPosition, rootItem);
          return;
        }
      } else if (oldValue instanceof Number) {
        boolean isSame = false;
        double fromDoubleValue = ((Number) oldValue).doubleValue();
        if (newValue instanceof Number) {
          double toDoubleValue = ((Number) newValue).doubleValue();
          isSame = (fromDoubleValue == toDoubleValue);
        }
        if (!isSame) {
          handleRangeList(updateList, rootPosition, rootItem);
          return;
        }
      } else if (oldValue instanceof String) {
        if (newValue == null || !TextUtils.equals(oldValue.toString(), newValue.toString())) {
          handleRangeList(updateList, rootPosition, rootItem);
          return;
        }
      } else if (oldValue instanceof HippyArray) {
        if (newValue instanceof HippyArray) {
          int sizeBefore = updateList.size();
          int innerSizeBefore = -1;
          RangeDiffItem item = null;
          if (sizeBefore > 0) {
            item = (RangeDiffItem) updateList.get(sizeBefore - 1);
            innerSizeBefore = item.getListSize();
          }
          diffArray((HippyArray) oldValue, (HippyArray) newValue, resultMap,
            diffLevel + 1, rootPosition, rootItem, listener, tClass);
          // update有新增，不需要继续循环
          if (sizeBefore != updateList.size() || innerSizeBefore != -1 && innerSizeBefore != item.getListSize()) {
            return;
          }
        } else {
          handleRangeList(updateList, rootPosition, rootItem);
          return;
        }
      } else if (oldValue instanceof HippyMap) {
        if (newValue instanceof HippyMap) {
          int sizeBefore = updateList.size();
          int innerSizeBefore = -1;
          RangeDiffItem item = null;
          if (sizeBefore > 0) {
            item = (RangeDiffItem) updateList.get(sizeBefore - 1);
            innerSizeBefore = item.getListSize();
          }
          diffMap((HippyMap) oldValue, (HippyMap) newValue, resultMap,
            diffLevel + 1, rootPosition, rootItem, listener, tClass);
          // update有新增，不需要继续循环
          if (sizeBefore != updateList.size() || innerSizeBefore != -1 && innerSizeBefore != item.getListSize()) {
            return;
          }
        } else {
          handleRangeList(updateList, rootPosition, rootItem);
          return;
        }
      }
    }
  }

}

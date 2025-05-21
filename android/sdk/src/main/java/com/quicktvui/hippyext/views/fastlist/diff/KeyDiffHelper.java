package com.quicktvui.hippyext.views.fastlist.diff;

import android.text.TextUtils;
import android.util.Log;

import com.tencent.mtt.hippy.common.HippyArray;
import com.tencent.mtt.hippy.common.HippyMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 有key diff实现类
 */
public class KeyDiffHelper {
  public static String KEY = "key";

  // 先处理有key的情况 key需要调用方自己控制处理，类型需要为String
  public static <T> Map<Integer, List<FastListDataBindingHelper.DiffItem>> handleKeyDataDiff(HippyArray beforeArray, HippyArray array, String keyName,
                                                                                             FastListDataBindingHelper.OnTransFormListener<T> listener, Class<T> tClass) {
    KEY = keyName;
    Map<Integer, List<FastListDataBindingHelper.DiffItem>> resultMap = new HashMap<>();

    if (beforeArray == null || array == null || beforeArray.size() < 1 || array.size() < 1) {
      resultMap.put(FastListDataBindingHelper.Patch.ALL_UPDATE, new ArrayList<>());
      return resultMap;
    }

    List<FastListDataBindingHelper.DiffItem> updateList = new ArrayList<>();
    List<FastListDataBindingHelper.DiffItem> deleteList = new ArrayList<>();
    List<FastListDataBindingHelper.DiffItem> insertList = new ArrayList<>();
    List<FastListDataBindingHelper.DiffItem> moveList = new ArrayList<>();
    resultMap.put(FastListDataBindingHelper.Patch.TYPE_UPDATE, updateList);
    resultMap.put(FastListDataBindingHelper.Patch.TYPE_DELETE, deleteList);
    resultMap.put(FastListDataBindingHelper.Patch.TYPE_INSERT, insertList);
    resultMap.put(FastListDataBindingHelper.Patch.TYPE_MOVE, moveList);

    realDiffByKey(beforeArray, array, resultMap, true, listener, tClass);

    return resultMap;
  }

  // 先不考虑被别人调用，判空逻辑基于已知调用，不过多处理
  private static <T> void realDiffByKey(HippyArray oldArray, HippyArray newArray,
                                        Map<Integer, List<FastListDataBindingHelper.DiffItem>> resultMap,
                                        boolean experimental,
                                        FastListDataBindingHelper.OnTransFormListener<T> listener, Class<T> tClass) {
    assert resultMap != null;

    List<FastListDataBindingHelper.DiffItem> updateList = resultMap.get(FastListDataBindingHelper.Patch.TYPE_UPDATE);
    List<FastListDataBindingHelper.DiffItem> deleteList = resultMap.get(FastListDataBindingHelper.Patch.TYPE_DELETE);
    List<FastListDataBindingHelper.DiffItem> insertList = resultMap.get(FastListDataBindingHelper.Patch.TYPE_INSERT);
    List<FastListDataBindingHelper.DiffItem> moveList = resultMap.get(FastListDataBindingHelper.Patch.TYPE_MOVE);

    assert updateList != null;
    assert deleteList != null;
    assert insertList != null;
    assert moveList != null;

    int i = 0, oldSize = oldArray.size(), newSize = newArray.size();

    // 第一步：先从头开始遍历 发现不同key就跳出
    Object o;
    while (i < oldSize && i < newSize) {
      o = transForm(oldArray.get(i), listener, tClass);
      if (isSameKey(o, newArray.get(i))) {
        // key相同 判断是否需要update
        handleUpdate(o, newArray.get(i), resultMap, i, newArray.get(i), false, listener, tClass);
      } else {
        break;
      }
      i++;
    }
    int oldLast = oldSize - 1, newLast = newSize - 1;
    // 第二步：从尾开始遍历 发现不同key跳出
    while (i <= oldLast && i <= newLast) {
      o = transForm(oldArray.get(oldLast), listener, tClass);
      if (isSameKey(o, newArray.get(newLast))) {
        // key相同 可以判断是否需要update
        handleUpdate(o, newArray.get(newLast), resultMap, oldLast, newArray.get(newLast), true, listener, tClass);
      } else {
        break;
      }
      oldLast--;
      newLast--;
    }
    // 第三步：假如新老数据中已经有遍历完的了
    if (i > oldLast) { // 旧数据已经遍历完，只要添加新数据就行
      while (i <= newLast) {
        FastListDataBindingHelper.RangeDiffItem.handleRangeList(insertList, i, newArray.get(i));
        i++;
      }

    } else if (i > newLast) {
      while (i <= oldLast) {
        FastListDataBindingHelper.RangeDiffItem.handleRangeList(deleteList, i, transForm(oldArray.get(i), listener, tClass));
        i++;
      }
    } else {
      if (!experimental) {
        resultMap.put(FastListDataBindingHelper.Patch.ALL_UPDATE, new ArrayList<>());
        return;
      }
      int s1 = i, s2 = i; // 记录第一步遍历到的index
      Map<String, Integer> keyToNewIndexMap = new HashMap<>(); // 保存新数据中未处理的数据 key：key值，value：数据位置
      for (i = s2; i <= newLast; i++) {
        if (realHasKey(newArray.get(i))) {
          keyToNewIndexMap.put((String) ((HippyMap) newArray.get(i)).get(KEY), i);
        } else {
          try {
            throw new IllegalArgumentException("使用有key的diff算法 每个item必须都有" + KEY + " new" + newArray.get(i));
          } catch (Exception e) {
            Log.e("KeyDiffHelper", Log.getStackTraceString(e));
          }
          resultMap.put(FastListDataBindingHelper.Patch.ALL_UPDATE, new ArrayList<>());
          return;
        }
      }
      int j, patched = 0, toBePatched = newLast - s2 + 1, maxNewIndexSoFar = 0; // toBePatched未处理的新数据个数
      boolean moved = false; // 是否有数据移动
      int[] newIndexToOldIndexArray = new int[toBePatched];
      for (i = s1; i <= oldLast; i++) { // 遍历未处理的旧数据
        Object oldItem = transForm(oldArray.get(i), listener, tClass);
        if (patched >= toBePatched) { // 新数据已经处理完，剩下的都是要删除的
          FastListDataBindingHelper.RangeDiffItem.handleRangeList(deleteList, i, oldItem);
          continue;
        }
        Integer newIndex;
        if (realHasKey(oldItem)) {
          newIndex = keyToNewIndexMap.get((String) ((HippyMap) oldItem).get(KEY));
        } else {
          try {
            throw new IllegalArgumentException("使用有key的diff算法 每个item必须都有" + KEY + " old" + oldItem);
          } catch (Exception e) {
            Log.e("KeyDiffHelper", Log.getStackTraceString(e));
          }
          resultMap.put(FastListDataBindingHelper.Patch.ALL_UPDATE, new ArrayList<>());
          return;
        }
        if (newIndex == null) {
          FastListDataBindingHelper.RangeDiffItem.handleRangeList(deleteList, i, oldItem);
        } else {
          newIndexToOldIndexArray[newIndex - s2] = i + 1; // 这里故意加1 区分0
          if (newIndex >= maxNewIndexSoFar) {
            maxNewIndexSoFar = newIndex;
          } else {
            moved = true;
          }
          patched++;
        }
      }
      // 处理新增的item和move的情况
      Integer[] increasingNewIndexSequence = moved ? getSequence(newIndexToOldIndexArray) : new Integer[0];
      j = increasingNewIndexSequence.length - 1;
      for (i = toBePatched - 1; i >= 0; i--) {
        int newIndex = i + s2;
        if (newIndexToOldIndexArray[i] == 0) {
          FastListDataBindingHelper.RangeDiffItem.handleRangeList(insertList, newIndex, newArray.get(newIndex), true);
        } else if (moved) {
          int oldIndex = newIndexToOldIndexArray[i] - 1;
          Object oldItem = transForm(oldArray.get(oldIndex), listener, tClass),
            newItem = newArray.get(newIndex);
          if (j < 0 || i != increasingNewIndexSequence[j]) {
            moveList.add(new FastListDataBindingHelper.MoveDiffItem(oldIndex, newIndex, oldItem, newItem));
          } else {
            handleUpdate(oldItem, newItem, resultMap, oldIndex, newItem, true, listener, tClass);
            j--;
          }
        }
      }
    }

  }

  private static <T> void handleUpdate(Object oldItem, Object newItem,
                                       Map<Integer, List<FastListDataBindingHelper.DiffItem>> resultMap,
                                       int rootPosition, Object rootItem, boolean isReverse,
                                       FastListDataBindingHelper.OnTransFormListener<T> listener, Class<T> tClass) {
    List<FastListDataBindingHelper.DiffItem> updateList = resultMap.get(FastListDataBindingHelper.Patch.TYPE_UPDATE);
    assert updateList != null;

    if (oldItem instanceof Boolean) {
      boolean fromBool = (boolean) oldItem;
      if (!(newItem instanceof Boolean) || fromBool != (boolean) newItem) {
        FastListDataBindingHelper.RangeDiffItem.handleRangeList(updateList, rootPosition, rootItem, isReverse);
      }
    } else if (oldItem instanceof Number) {
      boolean isSame = false;
      double fromDoubleValue = ((Number) oldItem).doubleValue();
      if (newItem instanceof Number) {
        double toDoubleValue = ((Number) newItem).doubleValue();
        isSame = (fromDoubleValue == toDoubleValue);
      }
      if (!isSame) {
        FastListDataBindingHelper.RangeDiffItem.handleRangeList(updateList, rootPosition, rootItem, isReverse);
      }
    } else if (oldItem instanceof String) {
      if (newItem == null || !TextUtils.equals(oldItem.toString(), newItem.toString())) {
        FastListDataBindingHelper.RangeDiffItem.handleRangeList(updateList, rootPosition, rootItem, isReverse);
      }
    } else if (oldItem instanceof HippyArray) {
      if (newItem instanceof HippyArray) {
        NoKeyDiffHelper.diffArray((HippyArray) oldItem, (HippyArray) newItem, resultMap, 1, rootPosition, rootItem, listener, tClass);
      } else {
        FastListDataBindingHelper.RangeDiffItem.handleRangeList(updateList, rootPosition, rootItem, isReverse);
      }
    } else if (oldItem instanceof HippyMap) {
      if (newItem instanceof HippyMap) {
        NoKeyDiffHelper.diffMap((HippyMap) oldItem, (HippyMap) newItem, resultMap,
          1, rootPosition, rootItem, listener, tClass);
      } else {
        FastListDataBindingHelper.RangeDiffItem.handleRangeList(updateList, rootPosition, rootItem, isReverse);
      }
    }
  }

  private static <T> Object transForm(Object item, FastListDataBindingHelper.OnTransFormListener<T> listener, Class<T> tClass) {
    if (listener != null && item.getClass() == tClass) {
      item = listener.onTransForm((T) item);
    }
    return item;
  }

  public static boolean realHasKey(Object o) {
    return (o instanceof HippyMap) && (((HippyMap) o).get(KEY) instanceof String)
      && !TextUtils.isEmpty((String) ((HippyMap) o).get(KEY));
  }

  public static boolean isSameKey(Object o1, Object o2) {
    if (realHasKey(o1) && realHasKey(o2)) {
      return ((String) ((HippyMap) o1).get(KEY)).equals(((HippyMap) o2).get(KEY));
    }
    return false;
  }

  private static Integer[] getSequence(int[] arr) {
    int[] p = arr.clone();
    List<Integer> result = new ArrayList<>();
    result.add(0);
    int i, j, u, v, c;
    int len = arr.length;
    for (i = 0; i < len; i++) {
      int arrI = arr[i];
      if (arrI != 0) {
        j = result.get(result.size() - 1);
        if (arr[j] < arrI) {
          p[i] = j;
          result.add(i);
          continue;
        }
        u = 0;
        v = result.size() - 1;
        // 二分查找
        while (u < v) {
          c = ((u + v) / 2);
          if (arr[result.get(c)] < arrI) {
            u = c + 1;
          } else {
            v = c;
          }
        }
        if (arrI < arr[result.get(u)]) {
          if (u > 0) {
            p[i] = result.get(u - 1);
          }
          result.set(u, i);
        }
      }
    }
    u = result.size();
    v = result.get(u - 1);
    while (u-- > 0) {
      result.set(u, v);
      v = p[v];
    }

    return result.toArray(new Integer[0]);
  }
}

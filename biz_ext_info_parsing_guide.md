# biz_ext_info字段解析SQL使用指南

## 概述
这个SQL脚本用于解析`biz_ext_info`字段中包含的键值对数据，将其拆分为独立的字段。

## 数据格式
原始数据格式：
```
PARENT_CATEGORY=生产数据 SUB_CATEGORY=XF-02灌装间空调机组采集信息表 CABIN_NAME= TAG=H02 CRON=0 0 * * * ?
```

## SQL解析方案

### 方案一：简单正则表达式（适用于值不包含空格的情况）
```sql
SELECT 
    biz_ext_info,
    REGEXP_EXTRACT(biz_ext_info, 'PARENT_CATEGORY=([^\\s]+)', 1) AS parent_category,
    REGEXP_EXTRACT(biz_ext_info, 'SUB_CATEGORY=([^\\s]+)', 1) AS sub_category,
    REGEXP_EXTRACT(biz_ext_info, 'CABIN_NAME=([^\\s]+)', 1) AS cabin_name,
    REGEXP_EXTRACT(biz_ext_info, 'TAG=([^\\s]+)', 1) AS tag,
    REGEXP_EXTRACT(biz_ext_info, 'CRON=([^\\s]+)', 1) AS cron
FROM your_table_name
WHERE biz_ext_info IS NOT NULL AND biz_ext_info != '';
```

### 方案二：精确正则表达式（适用于值可能包含空格的情况）
```sql
SELECT 
    biz_ext_info,
    REGEXP_EXTRACT(biz_ext_info, 'PARENT_CATEGORY=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS parent_category,
    REGEXP_EXTRACT(biz_ext_info, 'SUB_CATEGORY=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS sub_category,
    REGEXP_EXTRACT(biz_ext_info, 'CABIN_NAME=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS cabin_name,
    REGEXP_EXTRACT(biz_ext_info, 'TAG=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS tag,
    REGEXP_EXTRACT(biz_ext_info, 'CRON=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS cron
FROM your_table_name
WHERE biz_ext_info IS NOT NULL AND biz_ext_info != '';
```

## 正则表达式说明

### 方案一的正则表达式：
- `PARENT_CATEGORY=([^\\s]+)`：匹配`PARENT_CATEGORY=`后面直到空格的所有字符
- `[^\\s]+`：匹配一个或多个非空格字符

### 方案二的正则表达式：
- `PARENT_CATEGORY=([^=]+?)(?=\\s+[A-Z_]+=|$)`：更精确的匹配
  - `([^=]+?)`：非贪婪匹配，获取等号后的内容
  - `(?=\\s+[A-Z_]+=|$)`：正向先行断言，确保后面是空格+大写字母+下划线+等号，或者是字符串结尾

## 使用步骤

1. **替换表名**：将`your_table_name`替换为实际的表名
2. **选择方案**：根据数据特点选择方案一或方案二
3. **执行查询**：运行SQL获取解析结果

## 创建视图（可选）
如果需要频繁查询，可以创建一个视图：
```sql
CREATE VIEW parsed_biz_ext_info AS
SELECT 
    *,
    REGEXP_EXTRACT(biz_ext_info, 'PARENT_CATEGORY=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS parent_category,
    REGEXP_EXTRACT(biz_ext_info, 'SUB_CATEGORY=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS sub_category,
    REGEXP_EXTRACT(biz_ext_info, 'CABIN_NAME=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS cabin_name,
    REGEXP_EXTRACT(biz_ext_info, 'TAG=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS tag,
    REGEXP_EXTRACT(biz_ext_info, 'CRON=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS cron
FROM your_table_name
WHERE biz_ext_info IS NOT NULL AND biz_ext_info != '';
```

## 测试示例
使用提供的测试数据进行验证：
```sql
WITH test_data AS (
    SELECT 'PARENT_CATEGORY=生产数据 SUB_CATEGORY=XF-02灌装间空调机组采集信息表 CABIN_NAME= TAG=H02 CRON=0 0 * * * ?' AS biz_ext_info
)
SELECT 
    biz_ext_info,
    REGEXP_EXTRACT(biz_ext_info, 'PARENT_CATEGORY=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS parent_category,
    REGEXP_EXTRACT(biz_ext_info, 'SUB_CATEGORY=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS sub_category,
    REGEXP_EXTRACT(biz_ext_info, 'CABIN_NAME=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS cabin_name,
    REGEXP_EXTRACT(biz_ext_info, 'TAG=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS tag,
    REGEXP_EXTRACT(biz_ext_info, 'CRON=([^=]+?)(?=\\s+[A-Z_]+=|$)', 1) AS cron
FROM test_data;
```

## 预期结果
基于提供的测试数据，预期输出：
- parent_category: "生产数据"
- sub_category: "XF-02灌装间空调机组采集信息表"
- cabin_name: "" (空值)
- tag: "H02"
- cron: "0 0 * * * ?"

## 注意事项
1. 确保Doris版本支持`REGEXP_EXTRACT`函数
2. 根据实际数据格式调整正则表达式
3. 如果字段顺序不固定，可能需要使用更复杂的解析逻辑
4. 建议先在测试环境验证SQL的正确性


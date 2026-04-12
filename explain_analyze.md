# Оптимизация SQL-запросов

Анализ производительности запросов до и после добавления индексов в PostgreSQL.

## 📊 Запрос 1: Получение истории транзакций

### SQL-запрос:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM bonus_transactions t
JOIN bonus_cards c ON t.card_id = c.id
WHERE c.card_number = 'TEST-42'
ORDER BY t.created_at DESC;
```

### 📉 До добавления индексов:

```
Sort  (cost=3359.39..3361.84 rows=980 width=185) (actual time=23.976..24.154 rows=1026.00 loops=1)
  Sort Key: t.created_at DESC
  Sort Method: quicksort  Memory: 256kB
  Buffers: shared hit=2039
  ->  Hash Join  (cost=3.29..3310.70 rows=980 width=185) (actual time=0.091..23.216 rows=1026.00 loops=1)
        Hash Cond: (t.card_id = c.id)
        Buffers: shared hit=2036
        ->  Seq Scan on bonus_transactions t  (cost=0.00..3034.01 rows=100001 width=127) (actual time=0.029..9.706 rows=100001.00 loops=1)
              Buffers: shared hit=2034
        ->  Hash  (cost=3.28..3.28 rows=1 width=58) (actual time=0.029..0.030 rows=1.00 loops=1)
              Buckets: 1024  Batches: 1  Memory Usage: 9kB
              Buffers: shared hit=2
              ->  Seq Scan on bonus_cards c  (cost=0.00..3.28 rows=1 width=58) (actual time=0.016..0.022 rows=1.00 loops=1)
                    Filter: ((card_number)::text = 'TEST-42'::text)
                    Rows Removed by Filter: 101
                    Buffers: shared hit=2
Planning:
  Buffers: shared hit=315 dirtied=4
Planning Time: 2.513 ms
Execution Time: 24.289 ms
```

### 📈 После добавления индексов:

```
Sort  (cost=1784.88..1787.33 rows=980 width=185) (actual time=3.921..4.033 rows=1026.00 loops=1)
  Sort Key: t.created_at DESC
  Sort Method: quicksort  Memory: 256kB
  Buffers: shared hit=802 read=3
  ->  Nested Loop  (cost=12.04..1736.19 rows=980 width=185) (actual time=0.305..3.329 rows=1026.00 loops=1)
        Buffers: shared hit=799 read=3
        ->  Seq Scan on bonus_cards c  (cost=0.00..3.28 rows=1 width=58) (actual time=0.024..0.033 rows=1.00 loops=1)
              Filter: ((card_number)::text = 'TEST-42'::text)
              Rows Removed by Filter: 101
              Buffers: shared hit=2
        ->  Bitmap Heap Scan on bonus_transactions t  (cost=12.04..1722.91 rows=1000 width=127) (actual time=0.277..2.938 rows=1026.00 loops=1)
              Recheck Cond: (card_id = c.id)
              Heap Blocks: exact=797
              Buffers: shared hit=797 read=3
              ->  Bitmap Index Scan on idx_bonus_transactions_card_id  (cost=0.00..11.79 rows=1000 width=0) (actual time=0.178..0.179 rows=1026.00 loops=1)
                    Index Cond: (card_id = c.id)
                    Index Searches: 1
                    Buffers: shared read=3
Planning:
  Buffers: shared hit=188 read=7
Planning Time: 10.245 ms
Execution Time: 4.162 ms
```

### 🎯 Результат оптимизации:
**Запрос ускорен в 6 раз** (с 24.289 мс до 4.162 мс)

---

## 📊 Запрос 2: 10 самых активных карт по сумме транзакций

### SQL-запрос:
```sql
EXPLAIN (ANALYZE, BUFFERS, TIMING)
SELECT
    c.card_number,
    c.client_name,
    COUNT(t.id) as transaction_count,
    SUM(t.amount) as total_amount
FROM bonus_cards c
JOIN bonus_transactions t ON t.card_id = c.id
GROUP BY c.id, c.card_number, c.client_name
ORDER BY total_amount DESC
LIMIT 10;
```

### 📉 До добавления индексов:

```
Limit  (cost=121397.21..121397.23 rows=10 width=68) (actual time=2916.237..2954.137 rows=10.00 loops=1)
  Buffers: shared hit=773 read=21123 dirtied=17980 written=13395, temp read=377 written=378
  ->  Sort  (cost=121397.21..121647.46 rows=100102 width=68) (actual time=2916.235..2954.134 rows=10.00 loops=1)
        Sort Key: (sum(t.amount)) DESC
        Sort Method: top-N heapsort  Memory: 26kB
        Buffers: shared hit=773 read=21123 dirtied=17980 written=13395, temp read=377 written=378
        ->  Finalize GroupAggregate  (cost=82350.35..119234.04 rows=100102 width=68) (actual time=2893.851..2954.020 rows=100.00 loops=1)
              Group Key: c.card_number
              Buffers: shared hit=770 read=21123 dirtied=17980 written=13395, temp read=377 written=378
              ->  Gather Merge  (cost=82350.35..115580.31 rows=240245 width=68) (actual time=2893.616..2953.759 rows=275.00 loops=1)
                    Workers Planned: 2
                    Workers Launched: 2
                    Buffers: shared hit=770 read=21123 dirtied=17980 written=13395, temp read=377 written=378
                    ->  Partial GroupAggregate  (cost=81350.33..86850.06 rows=100102 width=68) (actual time=2639.533..2652.486 rows=91.67 loops=3)
                          Group Key: c.card_number
                          Buffers: shared hit=770 read=21123 dirtied=17980 written=13395, temp read=377 written=378
                          ->  Sort  (cost=81350.33..82412.44 rows=424845 width=42) (actual time=2639.464..2644.217 rows=33333.67 loops=3)
                                Sort Key: c.card_number
                                Sort Method: external merge  Disk: 3016kB
                                Buffers: shared hit=770 read=21123 dirtied=17980 written=13395, temp read=377 written=378
                                Worker 0:  Sort Method: quicksort  Memory: 4029kB
                                Worker 1:  Sort Method: quicksort  Memory: 34kB
                                ->  Parallel Hash Join  (cost=2462.89..28565.59 rows=424845 width=42) (actual time=861.313..2540.612 rows=33333.67 loops=3)
                                      Hash Cond: (t.card_id = c.id)
                                      Buffers: shared hit=754 read=21123 dirtied=17980 written=13395
                                      ->  Parallel Seq Scan on bonus_transactions t  (cost=0.00..24987.45 rows=424845 width=22) (actual time=825.045..2487.322 rows=33333.67 loops=3)
                                            Buffers: shared hit=740 read=19999 dirtied=17979 written=13395
                                      ->  Parallel Hash  (cost=1726.84..1726.84 rows=58884 width=28) (actual time=36.010..36.011 rows=33367.33 loops=3)
                                            Buckets: 131072  Batches: 1  Memory Usage: 7296kB
                                            Buffers: shared hit=14 read=1124 dirtied=1
                                            ->  Parallel Seq Scan on bonus_cards c  (cost=0.00..1726.84 rows=58884 width=28) (actual time=0.011..51.863 rows=100102.00 loops=1)
                                                  Buffers: shared hit=14 read=1124 dirtied=1
Planning:
  Buffers: shared hit=51 read=13
Planning Time: 1.849 ms
Execution Time: 2956.350 ms
```

### 📈 После добавления индексов:

```
Sort  (cost=15352.38..15354.88 rows=1000 width=68) (actual time=83.630..83.744 rows=10.00 loops=1)
  Sort Key: (sum(t.amount)) DESC
  Sort Method: top-N heapsort  Memory: 26kB
  Buffers: shared hit=2332 read=2
  ->  HashAggregate  (cost=15352.38..18360.23 rows=100001 width=68) (actual time=83.630..83.744 rows=100.00 loops=1)
        Group Key: c.card_number
        Planned Partitions: 8  Batches: 1  Memory Usage: 569kB
        Buffers: shared hit=2332 read=2
        ->  Nested Loop  (cost=0.30..5571.04 rows=100001 width=42) (actual time=0.069..57.237 rows=100001.00 loops=1)
              Buffers: shared hit=2332 read=2
              ->  Seq Scan on bonus_transactions t  (cost=0.00..3034.01 rows=100001 width=22) (actual time=0.020..6.859 rows=100001.00 loops=1)
                    Buffers: shared hit=2034
              ->  Memoize  (cost=0.30..0.38 rows=1 width=28) (actual time=0.000..0.000 rows=1.00 loops=100001)
                    Cache Key: t.card_id
                    Cache Mode: logical
                    Hits: 99901  Misses: 100  Evictions: 0  Overflows: 0  Memory Usage: 14kB
                    Buffers: shared hit=298 read=2
                    ->  Index Scan using bonus_cards_pkey on bonus_cards c  (cost=0.29..0.37 rows=1 width=28) (actual time=0.002..0.002 rows=1.00 loops=100)
                          Index Cond: (id = t.card_id)
                          Index Searches: 100
                          Buffers: shared hit=298 read=2
Planning:
  Buffers: shared hit=65 read=4
Planning Time: 2.208 ms
Execution Time: 84.147 ms
```

### 🎯 Результат оптимизации:
**Запрос ускорен примерно в 35 раз** (с 2956.350 мс до 84.147 мс)

---

## 📊 Запрос 3: Поиск по id заказа

### SQL-запрос:
```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT * FROM bonus_transactions WHERE order_id = 'ORDER-500000';
```

### 📉 До добавления индексов:

```
Gather  (cost=1000.00..40533.55 rows=1 width=136) (actual time=63.981..80.548 rows=2 loops=1)
  Workers Planned: 2
  Workers Launched: 2
  Buffers: shared hit=15665 read=18649 dirtied=17157 written=1396
  ->  Parallel Seq Scan on bonus_transactions  (cost=0.00..39533.45 rows=1 width=136) (actual time=50.794..71.009 rows=1 loops=3)
        Filter: ((order_id)::text = 'ORDER-500000'::text)
        Rows Removed by Filter: 666666
        Buffers: shared hit=15665 read=18649 dirtied=17157 written=1396
Planning:
  Buffers: shared hit=98 read=6 dirtied=1
Planning Time: 0.890 ms
Execution Time: 80.570 ms
```
### 📈 После добавления индексов:

```
Index Scan using idx_bonus_transactions_order_id on bonus_transactions  (cost=0.43..11.78 rows=2 width=136) (actual time=0.031..0.033 rows=2 loops=1)
  Index Cond: ((order_id)::text = 'ORDER-500000'::text)
  Buffers: shared hit=1 read=4
Planning:
  Buffers: shared hit=88 read=1
Planning Time: 0.607 ms
Execution Time: 0.052 ms
```

### 🎯 Результат оптимизации:
**Запрос ускорен примерно в 1549 раз** (с 80.570 мс мс до 0.052 мс)

## 📋 Итоги оптимизации

| Запрос                 | Время до оптимизации | Время после оптимизации | Ускорение |
|------------------------|---------------------|------------------------|-----------|
| История транзакций     | 24.289 мс | 4.162 мс | **6x**    |
| Топ-10 активных карт   | 2956.350 мс | 84.147 мс | **35x**   |
| Поиск по номеру заказа | 80.570 мс | 0.052 мс | **1549x** |
--A)----------------------------------------------------------------------------------------------------------------
WITH greater_then_10_in_2015 as (
    select
        c.CustomerID,
        si.ProductID,
        sum(si.amount) as Total
    from
        Customers c
        join Sales s on c.CustomerID = s.CustomerID
        join SalesItem si on s.TranID = si.TranID
    where
        EXTRACT(
            YEAR
            FROM
                s.DateTime
        ) = 2015
    group by
        c.CustomerID,
        si.ProductID
    having
        sum(si.amount) >= 10
)

select
    c.Name,
    p.Name,
    g.Total
from
    Customers c
    join greater_then_10_in_2015 g on c.CustomerID = g.CustomerID
    join Products p on p.ProductID = g.ProductID;

--B)----------------------------------------------------------------------------------------------------------------

WITH solditems_2010_2015_resume as (
select
    si.ProductID,
    sum(si.Amount) as Total
from
    Sales s
    join SalesItem si on s.TranID = si.TranID
where
    EXTRACT (YEAR FROM s.datetime ) between 2010 and 2015
group by si.ProductID)

select p.Name, COALESCE(r.Total, 0)
from Products p left join solditems_2010_2015_resume r on p.ProductID = r.ProductID
order by r.total, p.name DESC;

--C)----------------------------------------------------------------------------------------------------------------

WITH total_per_customer as (
    select s.CustomerID, sum(si.amount * pp.Price) as Total
    from
        Sales s
        join SalesItem si on s.TranID = si.TranID
        join ProductPrices pp on (
            pp.ProductID = si.ProductID
            and pp.ValidFrom = (SELECT ValidFrom
                    FROM
                        ProductPrices
                    WHERE
                        ProductID = pp.ProductID
                        AND ValidFrom <= s.DateTime
                    ORDER BY
                        ValidFrom DESC
                    LIMIT 1)
        )
    group by s.CustomerID
)

select c.CustomerID, c.name, t.Total
from Customers c join total_per_customer t on c.CustomerID = t.CustomerID
order by c.name;


--D)----------------------------------------------------------------------------------------------------------------
with number_of_distinct_buyers_per_year as (
    select productid, year, count(*) as distinct_buyers
    from (
        select
            distinct si.productid,
            s.customerid,
            extract(
                year
                from
                    datetime
            ) as year
        from
            sales s
            join salesitem si on s.tranid = si.tranid
    ) grouped_sells_per_year
    group by productid, year
),

number_of_unit_sold_per_year as (
        select
            extract(year from datetime) as year,
            si.productid,
            sum(amount) as total
        from
            sales s
            join salesitem si on s.tranid = si.tranid
        group by
            extract(year from datetime),
            si.productid
),

per_year_stats as (
select nu.year, nu.productid, nu.total, nb.distinct_buyers
from number_of_unit_sold_per_year nu join number_of_distinct_buyers_per_year nb on nu.year = nb.year and nu.productid = nb.productid
)

select pys.year, p.name, pys.total
from per_year_stats pys join (
    select year, max(total) as total, max(distinct_buyers) distinct_buyers 
    from per_year_stats
    group by year
) t on pys.year = t.year and pys.total = t.total and pys.distinct_buyers = t.distinct_buyers join Products p on pys.productid = p.ProductID;

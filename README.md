# stock-watch
[![996.icu](https://img.shields.io/badge/link-996.icu-red.svg)](https://996.icu)
[![LICENSE](https://img.shields.io/badge/license-Anti%20996-blue.svg)](https://github.com/996icu/996.ICU/blob/master/LICENSE)

在ide中可以查看股票行情的插件，光明正大摸鱼。

- 可以在底部status bar查看股票行情数据
- 股票代码 **#** 开头表示这一行不生效
- **ALT + Z**  功能开关快捷键
- 可以使用自定义脚本拉取数据
- 一行可以填写多个股票代码，使用 **,** 分割
- 添加趋势图预览


# 运行结果

![setting view](https://raw.githubusercontent.com/shenluw/stock-watch/master/img/settingview.jpg)

![status bar](https://raw.githubusercontent.com/shenluw/stock-watch/master/img/statusbar.jpg)
![趋势图](https://raw.githubusercontent.com/shenluw/stock-watch/master/img/trendchart.jpg)

## 显示格式化参数
例如：
 
    [${name} ${price} | ${percentage}%]
    显示结果 [苹果 10 | 1.1%]
    
    [${name:1} ${price} | ${percentage}%]
    显示结果 [苹 10 | 1.1%]   
    
    [${name:1,2} ${price} | ${percentage}%]
    显示结果 [果 10 | 1.1%]
~~~
变量约定:
    name             股票名称
    symbol           股票代码
    openPrice        开盘价
    preClose         上一日收盘价
    price            当前价格
    high             最高价
    low              最低价
    volume           成交量
    prePrice         盘前价
    afterPrice       盘后价
    percentage       涨跌幅
    prePercentage    盘前涨跌幅
    afterPercentage  盘后涨跌幅
    timestamp        更新时间
~~~
## 自定义脚本编写规则
~~~javascript
// 使用get请求获取数据
var httpMethod = "GET"

// symbols 股票代码
// 返回轮询地址
function processUrl(symbols) {
    return "http://xxxx.xxx"
}

// 解析接口返回数据
// text 解析返回值
// symbols 股票代码
// 返回股票数据json字符串
function parse(text, symbols) {
    // do some thing
    var infos = [
     {
        name: "股票名称",
        symbol: "股票代码",
        // 开盘价
        openPrice: "1.11",
        // 昨日收盘价
        preClose: "1.22",
        // 现价
        price: "1.21" , 
        // 最高价
        high: "2.11",
        // 最低价
        low: "1.1",
        // 成交量
        volume: "11111222",
        // 时间戳  毫秒
        timestamp: "129301939421",
        // 盘后价格
        afterPrice: "1.22",
        //盘前价格
        prePrice: "2.222"
     }
    ]   
    return JSON.stringify(infos)
}

// 点击状态栏时调用
// 可以显示返回链接所展示的图片
function trendChart(symbol, type) {
    console.info("sys" + symbol)
    return "http://xxxx.xxx/xxx.png"
}


// 配置更新时调用
function reset() {
    console.info("reset")
}
~~~
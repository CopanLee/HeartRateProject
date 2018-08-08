var express = require('express')
var sql = require('mssql')
var router = express.Router()

let pool

var config = {
    user: '',
    password: '',
    server: '',
    port: '',
    database: '',
}

async function connectSQL() {
    try {
        pool = await sql.connect(config);
    } catch (err) {
        console.log(err)
    }
}

function selectAllData() {
    try {
        if (pool != null) {
            return new Promise(async function (resolve, reject) {
                const request = await pool.request()
                request.query('select * from RR_interval', function (err, result) {
                    if (result.recordset.length > 0) {
                        resolve(result.recordset)
                    } else {
                        resolve("null")
                    }
                })
            })
        } else {
            console.log('Did\'n connect SQL')
        }
    } catch (err) {
        console.log(err)
    }
}

function selectIdData(id) {
    try {
        if (pool != null) {
            return new Promise(async function (resolve) {
                const request = await pool.request()
                request.input('DeviceID', sql.VarChar, id)
                request.query("SELECT * FROM RR_interval WHERE DeviceID = (@DeviceID)", function (err, result) {
                    if (result != undefined){
                        if (result.recordset.length > 0) {
                            resolve(result.recordset)
                        } else {
                            resolve("null")
                        }
                    }
                })

            })
        } else {
            console.log('Did\'n connect SQL')
        }
    } catch (err) {
        console.log(err)
    }
}
//Already fix 7/31
function selectDataWithDate(id, startDate, endDate) {
    try {
        if (pool != null) {
            return new Promise(async function (resolve) {
                const request = await pool.request()
                request.input('DeviceID', sql.VarChar, id)
                request.input('startDate', sql.VarChar, startDate)
                request.input('endDate', sql.VarChar, endDate)
                if(endDate != undefined){
                    queryString = 'SELECT DeviceData, StoreDate FROM RR_interval WHERE DeviceID = (@DeviceID) AND StoreDate >= (@startDate) AND StoreDate <= (@endDate)'
                } else {queryString = 'SELECT DeviceData, StoreDate FROM RR_interval WHERE DeviceID = (@DeviceID) AND StoreDate <= (@startDate)'}
                request.query(queryString, function (err, result) {
                    if (result != undefined) {
                        if (result.recordset.length > 0) {
                            resolve(result.recordset)
                        }else {
                            resolve('error')
                        }
                    } else {
                        resolve('error')
                    }
                })
            }).then(function(value){
                return value
            })
        } else {
            console.log('Did\'n connect SQL')
        }
    } catch (err) {
        console.log(err)
    }
}

function selectConnectHistoryDate(id) {
    try {
        if (pool != null) {
            return new Promise(async function (resolve) {
                const request = await pool.request()
                request.input('DeviceID', sql.VarChar, id)
                request.query("SELECT connectDate FROM connectHistory WHERE DeviceID = (@DeviceID) ORDER BY CONVERT(DateTime, connectDate, 101) DESC", function (err, result) {
                    if (result != undefined){
                        if (result.recordset.length > 0) {
                            resolve(result.recordset)
                        } else {
                            resolve('null')
                        }
                    }
                })
            })
        }
    } catch (err) {
        console.log(err)
    }
}

function selectRealTimeData(id, connectDate) {
    try {
        if (pool != null) {
            return new Promise(async function (resolve) {
                const request = await pool.request()
                request.input('DeviceID', sql.VarChar, id)
                request.input('connectDate', sql.VarChar, connectDate)
                request.query("SELECT * FROM RR_interval WHERE DeviceID = (@DeviceID) and StoreDate >= (@connectDate)", function (err, result) {
                    if (result != undefined){
                        if (result.recordset.length > 0) {
                            resolve(result.recordset)
                        } else {
                            resolve('null')
                        }
                    }
                })
            })
        }
    } catch (err) {
        console.log(err)
    }
}

function selectConnectionDevice() {
    try {
        if (pool != null) {
            return new Promise(async function (resolve) {
                const request = await pool.request()
                request.query("SELECT DeviceID FROM connectionDevice", function (err, result) {
                    if (result.recordset.length > 0) {
                        resolve(result.recordset)
                    } else {
                        resolve('null')
                    }
                })
            })
        }
    } catch (err) {
        console.log(err)
    }
}

function selectDeviceDistinct() {
    try {
        if (pool != null) {
            return new Promise(async function (resolve) {
                const request = await pool.request()
                request.query("SELECT DISTINCT DeviceID FROM connectHistory", function (err, result) {
                    if (result != undefined){
                        if (result.recordset.length > 0) {
                            resolve(result.recordset)
                        } else {
                            resolve('null')
                        }
                    }
                })
            })
        }
    } catch (err) {
        console.log(err)
    }
}

connectSQL()

//return error
router.get('/', function (req, res, next) {
    res.json({
        message: "error"
    })
})

//return data
router.get('/all', function (req, res, next) {
    selectAllData().then(function (result) {
        if (result != 'null') {
            res.json({
                message: "successful",
                allData: result,
            })
        } else {
            res.json({
                message: result,
            })
        }
    }, function (err) {
        console.log(err)
    })
})

router.get('/select', function (req, res, next) {
    selectIdData(req.query.id).then(function (result) {
        if (result != 'null') {
            var dataArray = [], storeDateArray = [];
            result.forEach(function (item) {
                dataArray.push(item['DeviceData'])
                storeDateArray.push(item['StoreDate'])
            })
            res.json({
                selectId: req.query.id,
                message: "successful",
                data: dataArray,
                date: storeDateArray
            })
        } else {
            res.json({
                selectId: req.query.id,
                message: 'ID error',
                data: result,
                date: result
            })
        }
    }, function (err) {
        console.log(err)
    })
});

router.get('/device', function (req, res, next) {
    selectConnectionDevice().then(function (result) {
        if (result != 'null') {
            var deviceArray = []
            result.forEach(function (item) {
                deviceArray.push(item['DeviceID'])
            })
            res.json({
                message: "successful",
                deviceId: deviceArray
            })
        } else {
            res.json({
                message: "error",
                deviceId: deviceArray
            })
        }
    }, function (err) {
        console.log(err)
    })
})

router.get('/alldevice', function (req, res, next) {
    selectDeviceDistinct().then(function (result) {
        if (result != 'null') {
            var deviceArray = []
            result.forEach(function (item) {
                deviceArray.push(item['DeviceID'])
            })
            res.json({
                message: "successful",
                deviceId: deviceArray
            })
        } else {
            res.json({
                message: "error",
                deviceId: deviceArray
            })
        }
    }, function (err) {
        console.log(err)
    })
})

router.get('/realtime', function (req, res, next) {
    selectRealTimeData(req.query.id, req.query.date).then(function (resultData) {
        if (resultData != 'null') {
            var dataArray = [], storeDateArray = [];
            resultData.forEach(function (item) {
                dataArray.push(item['DeviceData'])
                storeDateArray.push(item['StoreDate'])
            })
            res.json({
                selectId: req.query.id,
                message: 'successful',
                data: dataArray,
                date: storeDateArray
            })
        } else {
            res.json({
                selectId: req.query.id,
                message: 'error',
            })
        }
    }, function (err) {
        console.log(err)
    })
});

router.get('/history', function (req, res, next) {
    selectConnectHistoryDate(req.query.id).then(function (result) {
        if (result != 'null') {
            var historyArray = []
            result.forEach(function(item){
                historyArray.push(item['connectDate'])
            })
            res.json({
                selectId: req.query.id,
                historyDate: historyArray,
            })
        }else{
            res.json({
                selectId: req.query.id,
                historyDate: 'null',
            })
        }
    }, function (err) {
        console.log(err)
    })
})
//Already fix 7/31
router.get('/data', function (req, res, next) {
    selectDataWithDate(req.query.id, req.query.start, req.query.end).then(function (result) {
        if (result != 'error') {
            var dataArray = [], dateArray = []
            result.forEach(function(item){
                dataArray.push(item['DeviceData'])
                dateArray.push(item['StoreDate'])
            })
            var recordingTime = (new Date(dateArray[dateArray.length-1]).getTime() / 1000 - new Date(dateArray[0]).getTime() / 1000).toFixed(3)
            res.json({
                selectId: req.query.id,
                //dateRange: dateArray[0] + ' ~ ' + dateArray[dateArray.length-1],
                dataCount: dataArray.length,
                recordingTime: recordingTime,
                dataArray: dataArray,
                dateArray: dateArray
            })
        }else {
            res.json({
                selectId: req.query.id,
                dateRange: req.query.start + ' ~ ' + req.query.end,
                dataArray: 'null',
                dateArray: 'null'
            })
        }
    }, function (err) {
        console.log(err)
    })
})


module.exports = router;
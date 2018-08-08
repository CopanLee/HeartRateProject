var express = require('express');
var router = express.Router();

router.get('/', function(req, res, next) {
  res.render('hrv', { title: '心跳分析頁面' });
});

module.exports = router;

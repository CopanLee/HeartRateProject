var express = require('express');
var router = express.Router();

router.get('/', function (req, res, next) {
  res.render('realtime', { title: '即時心率狀況' });
});

module.exports = router;

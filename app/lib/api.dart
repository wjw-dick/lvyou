import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';

/// 统一 API 客户端：UnionID 登录态与小程序/公众号/H5 同源。
class Api {
  static const String baseUrl = String.fromEnvironment('API_BASE', defaultValue: 'http://10.0.2.2:8080/api');
  static const String tenantCode = String.fromEnvironment('TENANT_CODE', defaultValue: 'HLJ');

  static String _token = '';

  static Future<void> loadToken() async {
    final prefs = await SharedPreferences.getInstance();
    _token = prefs.getString('tp_token') ?? '';
  }

  static Future<void> saveToken(String token) async {
    _token = token;
    final prefs = await SharedPreferences.getInstance();
    await prefs.setString('tp_token', token);
  }

  static Future<void> logout() async {
    _token = '';
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('tp_token');
  }

  static Map<String, String> get _headers => {
        'Content-Type': 'application/json',
        'X-Tenant-Code': tenantCode,
        if (_token.isNotEmpty) 'Authorization': 'Bearer $_token',
      };

  static Future<dynamic> get(String path, [Map<String, String>? query]) async {
    final uri = Uri.parse('$baseUrl$path').replace(queryParameters: query);
    final res = await http.get(uri, headers: _headers);
    return _unwrap(res);
  }

  static Future<dynamic> post(String path, [Map<String, dynamic>? body]) async {
    final res = await http.post(Uri.parse('$baseUrl$path'),
        headers: _headers, body: jsonEncode(body ?? {}));
    return _unwrap(res);
  }

  static dynamic _unwrap(http.Response res) {
    final body = jsonDecode(utf8.decode(res.bodyBytes));
    if (body is Map && body['code'] == 0) return body['data'];
    throw Exception(body is Map ? (body['message'] ?? '请求失败') : '请求失败');
  }

  // ---------- 业务接口 ----------
  static Future<dynamic> tenant() => get('/public/tenant');
  static Future<List<dynamic>> products({String? keyword}) async {
    final data = await get('/public/products', {
      'page': '1',
      'size': '20',
      if (keyword != null && keyword.isNotEmpty) 'keyword': keyword,
    });
    return data['records'] as List<dynamic>;
  }

  static Future<dynamic> product(int id) => get('/public/products/$id');
  static Future<List<dynamic>> destinations() async => await get('/public/destinations') as List<dynamic>;
  static Future<dynamic> createOrder(Map<String, dynamic> body) => post('/public/orders', body);
  static Future<dynamic> memberLogin(String code, String nickname) =>
      post('/auth/member-login', {'tenantCode': tenantCode, 'code': code, 'nickname': nickname});
  static Future<List<dynamic>> myOrders() async => await get('/member/orders') as List<dynamic>;
  static Future<dynamic> myProfile() => get('/member/profile');
  static Future<List<dynamic>> myCoupons() async => await get('/member/coupons') as List<dynamic>;
  static Future<dynamic> guideSchedules() => get('/guide/app/schedules');
  static Future<dynamic> guideTourists(int scheduleId) => get('/guide/app/schedules/$scheduleId/tourists');
  static Future<dynamic> guideReport(int scheduleId) => get('/guide/app/report', {'scheduleId': '$scheduleId'});
  static Future<dynamic> submitExpense(Map<String, dynamic> body) => post('/guide/app/expenses', body);
}

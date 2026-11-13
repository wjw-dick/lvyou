import 'package:flutter/material.dart';

import '../api.dart';
import 'guide_page.dart';

class MinePage extends StatefulWidget {
  const MinePage({super.key});

  @override
  State<MinePage> createState() => _MinePageState();
}

class _MinePageState extends State<MinePage> {
  Map<String, dynamic> _profile = {};
  List<dynamic> _coupons = [];
  bool _logged = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final profile = await Api.myProfile();
      final coupons = await Api.myCoupons();
      setState(() {
        _profile = Map<String, dynamic>.from(profile ?? {});
        _coupons = coupons;
        _logged = true;
      });
    } catch (_) {
      setState(() => _logged = false);
    }
  }

  Future<void> _login() async {
    try {
      final result = await Api.memberLogin('flutter-demo-${DateTime.now().millisecondsSinceEpoch}', 'App 游客');
      await Api.saveToken(result['token']);
      _load();
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(e.toString())));
    }
  }

  @override
  Widget build(BuildContext context) {
    final member = _profile['member'] ?? {};
    return Scaffold(
      appBar: AppBar(title: const Text('我的'), backgroundColor: Colors.white),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              gradient: const LinearGradient(colors: [Color(0xFF1F6FEB), Color(0xFF38BDF8)]),
              borderRadius: BorderRadius.circular(16),
            ),
            child: Row(
              children: [
                CircleAvatar(
                  radius: 30,
                  backgroundColor: Colors.white24,
                  child: Text((member['nickname'] ?? '游').toString().substring(0, 1),
                      style: const TextStyle(color: Colors.white, fontSize: 22)),
                ),
                const SizedBox(width: 14),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(member['nickname'] ?? '点击登录',
                          style: const TextStyle(color: Colors.white, fontSize: 18, fontWeight: FontWeight.bold)),
                      Text(member['unionId'] ?? 'UnionID 多端互通',
                          style: const TextStyle(color: Color(0xFFE0F2FE), fontSize: 12)),
                      const SizedBox(height: 6),
                      Text('L${member['level'] ?? 1} · 积分 ${member['points'] ?? 0}',
                          style: const TextStyle(color: Colors.white, fontSize: 12)),
                    ],
                  ),
                ),
                if (!_logged) TextButton(onPressed: _login, child: const Text('登录', style: TextStyle(color: Colors.white))),
              ],
            ),
          ),
          const SizedBox(height: 16),
          _tile(Icons.card_giftcard, '优惠券（${_coupons.length}）', () {}),
          _tile(Icons.work_outline, '导游端（带团/点名/报账）',
              () => Navigator.push(context, MaterialPageRoute(builder: (_) => const GuidePage()))),
          _tile(Icons.support_agent, '客服电话 400-800-1234', () {}),
          _tile(Icons.privacy_tip_outlined, '隐私协议 / 用户协议', () {}),
          _tile(Icons.logout, '退出登录', () async {
            await Api.logout();
            setState(() => _logged = false);
          }),
        ],
      ),
    );
  }

  Widget _tile(IconData icon, String title, VoidCallback onTap) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      child: ListTile(
        tileColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
        leading: Icon(icon, color: const Color(0xFF1F6FEB)),
        title: Text(title),
        trailing: const Icon(Icons.chevron_right, color: Color(0xFFC0C4CC)),
        onTap: onTap,
      ),
    );
  }
}

import 'package:flutter/material.dart';

import '../api.dart';

class GuidePage extends StatefulWidget {
  const GuidePage({super.key});

  @override
  State<GuidePage> createState() => _GuidePageState();
}

class _GuidePageState extends State<GuidePage> {
  List<dynamic> _schedules = [];
  List<dynamic> _tourists = [];
  Map<String, dynamic> _report = {};
  int? _selectedId;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final data = await Api.guideSchedules();
      setState(() => _schedules = data ?? []);
      if (_schedules.isNotEmpty) _select(_schedules.first['scheduleId']);
    } catch (_) {}
  }

  Future<void> _select(int scheduleId) async {
    try {
      final tourists = await Api.guideTourists(scheduleId);
      final report = await Api.guideReport(scheduleId);
      setState(() {
        _selectedId = scheduleId;
        _tourists = tourists ?? [];
        _report = Map<String, dynamic>.from(report ?? {});
      });
    } catch (_) {}
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('导游端'), backgroundColor: Colors.white),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          const Text('带团行程', style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
          const SizedBox(height: 10),
          ..._schedules.map((s) {
            final active = _selectedId == s['scheduleId'];
            return GestureDetector(
              onTap: () => _select(s['scheduleId']),
              child: Container(
                margin: const EdgeInsets.only(bottom: 10),
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(12),
                  border: Border.all(color: active ? const Color(0xFF1F6FEB) : Colors.transparent),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(s['productTitle'] ?? '', style: const TextStyle(fontWeight: FontWeight.w600)),
                    const SizedBox(height: 4),
                    Text('${s['scheduleNo']} · ${s['departDate']}',
                        style: const TextStyle(color: Color(0xFF909399), fontSize: 12)),
                    Text('集合：${s['meetingPoint'] ?? ''} ${s['meetingTime'] ?? ''}',
                        style: const TextStyle(color: Color(0xFF909399), fontSize: 12)),
                  ],
                ),
              ),
            );
          }),
          if (_schedules.isEmpty)
            const Padding(padding: EdgeInsets.all(30), child: Center(child: Text('暂无带团安排'))),
          const SizedBox(height: 12),
          if (_selectedId != null) ...[
            Row(children: [
              const Text('游客名单', style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold)),
              const Spacer(),
              TextButton(
                onPressed: () async {
                  await Api.post('/guide/app/schedules/$_selectedId/rollcall', {'present': _tourists.length});
                  if (!mounted) return;
                  ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(content: Text('点名完成：应到 ${_tourists.length} 人')));
                },
                child: const Text('客人点名'),
              ),
            ]),
            ..._tourists.map((t) => ListTile(
                  tileColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                  title: Text(t['name'] ?? ''),
                  subtitle: Text('${t['idNoMask'] ?? ''} · ${t['phoneMask'] ?? ''}'),
                  trailing: Chip(
                    label: Text(t['verifyStatus'] == 1 ? '已核销' : '未核销',
                        style: const TextStyle(fontSize: 11)),
                    visualDensity: VisualDensity.compact,
                  ),
                )),
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(12)),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('团报汇总', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 8),
                  Text('订单数：${_report['orderCount'] ?? 0}'),
                  Text('报账：¥${_report['expenseAmount'] ?? 0}'),
                  Text('现金领款：¥${_report['cashAmount'] ?? 0}'),
                  Text('结余：¥${_report['balance'] ?? 0}'),
                ],
              ),
            ),
          ],
        ],
      ),
    );
  }
}

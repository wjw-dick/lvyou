import 'package:flutter/material.dart';

import '../api.dart';

class ProductDetailPage extends StatefulWidget {
  final int productId;
  const ProductDetailPage({super.key, required this.productId});

  @override
  State<ProductDetailPage> createState() => _ProductDetailPageState();
}

class _ProductDetailPageState extends State<ProductDetailPage> {
  Map<String, dynamic>? _product;
  List<dynamic> _itineraries = [];
  List<dynamic> _schedules = [];
  int? _scheduleId;
  int _adult = 1;
  int _child = 0;
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _idNo = TextEditingController();
  bool _submitting = false;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    final data = await Api.product(widget.productId);
    final schedules = await Api.get('/public/products/${widget.productId}/schedules');
    setState(() {
      _product = Map<String, dynamic>.from(data['product']);
      _itineraries = data['itineraries'] ?? [];
      _schedules = schedules ?? [];
      if (_schedules.isNotEmpty) _scheduleId = _schedules.first['id'];
    });
  }

  Future<void> _book() async {
    if (_scheduleId == null) return _toast('请选择团期');
    if (_name.text.isEmpty || _phone.text.isEmpty) return _toast('请填写联系人信息');
    setState(() => _submitting = true);
    try {
      final order = await Api.createOrder({
        'productId': widget.productId,
        'scheduleId': _scheduleId,
        'adultCount': _adult,
        'childCount': _child,
        'contactName': _name.text,
        'contactPhone': _phone.text,
        'tourists': [
          {'name': _name.text, 'idType': 'id_card', 'idNo': _idNo.text, 'phone': _phone.text, 'isAdult': 1}
        ],
        'channel': 'app'
      });
      if (!mounted) return;
      showDialog(
        context: context,
        builder: (_) => AlertDialog(
          title: const Text('预订成功'),
          content: Text('订单号：${order['orderNo']}\n请在小程序/公众号/H5 完成支付。'),
          actions: [TextButton(onPressed: () => Navigator.pop(_), child: const Text('好的'))],
        ),
      );
    } catch (e) {
      _toast(e.toString());
    }
    setState(() => _submitting = false);
  }

  void _toast(String msg) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(msg)));
  }

  @override
  Widget build(BuildContext context) {
    if (_product == null) {
      return const Scaffold(body: Center(child: CircularProgressIndicator()));
    }
    return Scaffold(
      appBar: AppBar(title: Text(_product!['title'] ?? '线路详情'), backgroundColor: Colors.white),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Container(
            height: 160,
            decoration: BoxDecoration(
              color: const Color(0xFFDBEAFE),
              borderRadius: BorderRadius.circular(14),
            ),
            alignment: Alignment.center,
            child: Text((_product!['title'] ?? '').toString().substring(0, 2),
                style: const TextStyle(color: Color(0xFF1F6FEB), fontSize: 40)),
          ),
          const SizedBox(height: 14),
          Text(_product!['title'] ?? '', style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          Text(_product!['subtitle'] ?? '', style: const TextStyle(color: Color(0xFF909399), fontSize: 13)),
          const SizedBox(height: 10),
          Text('¥${_product!['adultPrice']} 起/人 · 儿童 ¥${_product!['childPrice']}',
              style: const TextStyle(color: Color(0xFFF56C6C), fontSize: 20, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          const Text('选择团期', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: _schedules.map((s) {
              final active = _scheduleId == s['id'];
              return ChoiceChip(
                label: Text('${s['departDate']} 余${s['left']}'),
                selected: active,
                onSelected: (_) => setState(() => _scheduleId = s['id']),
              );
            }).toList(),
          ),
          const SizedBox(height: 16),
          const Text('行程安排', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          ..._itineraries.map((it) => ListTile(
                contentPadding: EdgeInsets.zero,
                leading: CircleAvatar(
                  backgroundColor: const Color(0xFFE8F1FF),
                  child: Text('D${it['dayNo']}', style: const TextStyle(color: Color(0xFF1F6FEB), fontSize: 12)),
                ),
                title: Text(it['title'] ?? ''),
                subtitle: Text('${it['spots'] ?? ''} · ${it['hotel'] ?? ''}',
                    style: const TextStyle(fontSize: 12)),
              )),
          const Divider(height: 32),
          const Text('预订信息', style: TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(child: _counter('成人', _adult, (v) => setState(() => _adult = v))),
              const SizedBox(width: 12),
              Expanded(child: _counter('儿童', _child, (v) => setState(() => _child = v))),
            ],
          ),
          const SizedBox(height: 10),
          TextField(controller: _name, decoration: const InputDecoration(labelText: '联系人姓名')),
          const SizedBox(height: 10),
          TextField(controller: _phone, keyboardType: TextInputType.phone,
              decoration: const InputDecoration(labelText: '手机号')),
          const SizedBox(height: 10),
          TextField(controller: _idNo, decoration: const InputDecoration(labelText: '证件号（加密存储）')),
          const SizedBox(height: 16),
          SizedBox(
            height: 48,
            child: FilledButton(
              onPressed: _submitting ? null : _book,
              child: Text(_submitting ? '提交中...' : '提交预订'),
            ),
          ),
          const SizedBox(height: 30),
        ],
      ),
    );
  }

  Widget _counter(String label, int value, ValueChanged<int> onChange) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(8)),
      child: Row(
        children: [
          Text(label),
          const Spacer(),
          IconButton(onPressed: value > 0 ? () => onChange(value - 1) : null, icon: const Icon(Icons.remove_circle_outline)),
          Text('$value'),
          IconButton(onPressed: () => onChange(value + 1), icon: const Icon(Icons.add_circle_outline)),
        ],
      ),
    );
  }
}

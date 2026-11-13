import 'package:flutter/material.dart';

import '../api.dart';

class OrdersPage extends StatefulWidget {
  const OrdersPage({super.key});

  @override
  State<OrdersPage> createState() => _OrdersPageState();
}

class _OrdersPageState extends State<OrdersPage> {
  static const _statusMap = {
    1: '待支付',
    2: '待确认',
    3: '已成团',
    4: '待出团',
    5: '已出团',
    6: '已核销',
    7: '已完结',
    8: '已取消'
  };
  List<dynamic> _orders = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      _orders = await Api.myOrders();
    } catch (_) {
      _orders = [];
    }
    setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('我的订单'), backgroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : RefreshIndicator(
              onRefresh: _load,
              child: _orders.isEmpty
                  ? ListView(children: const [
                      SizedBox(height: 160),
                      Center(child: Text('暂无订单', style: TextStyle(color: Color(0xFF909399)))),
                    ])
                  : ListView.builder(
                      padding: const EdgeInsets.all(16),
                      itemCount: _orders.length,
                      itemBuilder: (_, i) {
                        final o = _orders[i];
                        return Container(
                          margin: const EdgeInsets.only(bottom: 12),
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14)),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Row(children: [
                                Expanded(
                                    child: Text(o['productTitle'] ?? '',
                                        style: const TextStyle(fontWeight: FontWeight.w600))),
                                Chip(
                                  label: Text(_statusMap[o['status']] ?? '',
                                      style: const TextStyle(fontSize: 11)),
                                  visualDensity: VisualDensity.compact,
                                )
                              ]),
                              const SizedBox(height: 4),
                              Text(o['orderNo'] ?? '', style: const TextStyle(color: Color(0xFF909399), fontSize: 12)),
                              Text('${o['departDate']} · ${o['adultCount']}大${o['childCount']}小',
                                  style: const TextStyle(color: Color(0xFF909399), fontSize: 12)),
                              const SizedBox(height: 8),
                              Row(children: [
                                Text('¥${o['paidAmount'] ?? o['totalAmount']}',
                                    style: const TextStyle(
                                        color: Color(0xFFF56C6C),
                                        fontSize: 17,
                                        fontWeight: FontWeight.bold)),
                                const Spacer(),
                                if (o['payStatus'] == 0)
                                  FilledButton.tonal(
                                    onPressed: () async {
                                      await Api.post('/member/orders/${o['id']}/pay');
                                      _load();
                                    },
                                    child: const Text('去支付'),
                                  )
                              ]),
                            ],
                          ),
                        );
                      },
                    ),
            ),
    );
  }
}

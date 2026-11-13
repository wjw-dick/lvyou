import 'package:flutter/material.dart';

import '../api.dart';
import 'product_detail_page.dart';

class ProductsPage extends StatefulWidget {
  const ProductsPage({super.key});

  @override
  State<ProductsPage> createState() => _ProductsPageState();
}

class _ProductsPageState extends State<ProductsPage> {
  List<dynamic> _products = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      _products = await Api.products();
    } catch (_) {}
    setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('全部线路'), backgroundColor: Colors.white),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : ListView.builder(
              padding: const EdgeInsets.all(16),
              itemCount: _products.length,
              itemBuilder: (_, i) {
                final p = _products[i];
                return GestureDetector(
                  onTap: () => Navigator.push(context,
                      MaterialPageRoute(builder: (_) => ProductDetailPage(productId: p['id']))),
                  child: Container(
                    margin: const EdgeInsets.only(bottom: 12),
                    padding: const EdgeInsets.all(14),
                    decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14)),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(p['title'] ?? '', style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w600)),
                        const SizedBox(height: 6),
                        Text('${p['destination'] ?? ''} · ${p['days']}天 · 余位 ${(p['stock'] ?? 0) - (p['sold'] ?? 0)}',
                            style: const TextStyle(color: Color(0xFF909399), fontSize: 12)),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Text('¥${p['adultPrice']}',
                                style: const TextStyle(color: Color(0xFFF56C6C), fontSize: 18, fontWeight: FontWeight.bold)),
                            const SizedBox(width: 4),
                            const Text('起/人', style: TextStyle(color: Color(0xFF909399), fontSize: 11)),
                            const Spacer(),
                            const Text('查看详情 >', style: TextStyle(color: Color(0xFF1F6FEB), fontSize: 12)),
                          ],
                        ),
                      ],
                    ),
                  ),
                );
              },
            ),
    );
  }
}

import 'package:flutter/material.dart';

import '../api.dart';
import 'product_detail_page.dart';

class HomePage extends StatefulWidget {
  const HomePage({super.key});

  @override
  State<HomePage> createState() => _HomePageState();
}

class _HomePageState extends State<HomePage> {
  List<dynamic> _products = [];
  List<dynamic> _destinations = [];
  Map<String, dynamic> _tenant = {};
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _load();
  }

  Future<void> _load() async {
    try {
      final tenant = await Api.tenant();
      final products = await Api.products();
      final destinations = await Api.destinations();
      setState(() {
        _tenant = Map<String, dynamic>.from(tenant ?? {});
        _products = products;
        _destinations = destinations.take(6).toList();
      });
    } catch (_) {}
    setState(() => _loading = false);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: RefreshIndicator(
        onRefresh: _load,
        child: ListView(
          children: [
            Container(
              padding: const EdgeInsets.fromLTRB(20, 70, 20, 28),
              decoration: const BoxDecoration(
                gradient: LinearGradient(
                  colors: [Color(0xFF0F172A), Color(0xFF1E3A8A)],
                  begin: Alignment.topLeft,
                  end: Alignment.bottomRight,
                ),
                borderRadius: BorderRadius.vertical(bottom: Radius.circular(24)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(_tenant['name'] ?? '雪乡国旅',
                      style: const TextStyle(color: Colors.white, fontSize: 24, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 6),
                  const Text('冰雪季 · 雪乡 / 哈尔滨 · 纯玩无购物',
                      style: TextStyle(color: Color(0xFFC7D2FE), fontSize: 13)),
                  const SizedBox(height: 18),
                  TextField(
                    decoration: const InputDecoration(
                      hintText: '搜索线路 / 目的地',
                      prefixIcon: Icon(Icons.search, color: Color(0xFF909399)),
                    ),
                    onSubmitted: (v) async {
                      final data = await Api.products(keyword: v);
                      if (!mounted) return;
                      Navigator.push(context, MaterialPageRoute(builder: (_) => _SearchResultPage(products: data)));
                    },
                  ),
                ],
              ),
            ),
            Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('热门目的地', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 12),
                  SizedBox(
                    height: 96,
                    child: ListView.separated(
                      scrollDirection: Axis.horizontal,
                      itemCount: _destinations.length,
                      separatorBuilder: (_, __) => const SizedBox(width: 12),
                      itemBuilder: (_, i) {
                        final d = _destinations[i];
                        return Container(
                          width: 110,
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: Colors.white,
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Text(d['name'] ?? '', style: const TextStyle(fontWeight: FontWeight.w600)),
                              const SizedBox(height: 4),
                              Text(d['city'] ?? '', style: const TextStyle(color: Color(0xFF909399), fontSize: 12)),
                            ],
                          ),
                        );
                      },
                    ),
                  ),
                  const SizedBox(height: 20),
                  const Text('热门线路', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 12),
                  if (_loading) const Center(child: Padding(padding: EdgeInsets.all(30), child: CircularProgressIndicator())),
                  ..._products.map((p) => _ProductCard(product: p)),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _ProductCard extends StatelessWidget {
  final dynamic product;
  const _ProductCard({required this.product});

  @override
  Widget build(BuildContext context) {
    return GestureDetector(
      onTap: () => Navigator.push(context,
          MaterialPageRoute(builder: (_) => ProductDetailPage(productId: product['id']))),
      child: Container(
        margin: const EdgeInsets.only(bottom: 12),
        padding: const EdgeInsets.all(12),
        decoration: BoxDecoration(color: Colors.white, borderRadius: BorderRadius.circular(14)),
        child: Row(
          children: [
            Container(
              width: 96,
              height: 78,
              decoration: BoxDecoration(
                borderRadius: BorderRadius.circular(10),
                color: const Color(0xFFDBEAFE),
              ),
              alignment: Alignment.center,
              child: Text((product['title'] ?? '线路').toString().substring(0, 1),
                  style: const TextStyle(color: Color(0xFF1F6FEB), fontSize: 26)),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(product['title'] ?? '', maxLines: 1, overflow: TextOverflow.ellipsis,
                      style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 15)),
                  const SizedBox(height: 4),
                  Text('${product['destination'] ?? ''} · ${product['days']}天',
                      style: const TextStyle(color: Color(0xFF909399), fontSize: 12)),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Text('¥${product['adultPrice']}',
                          style: const TextStyle(color: Color(0xFFF56C6C), fontWeight: FontWeight.bold, fontSize: 16)),
                      const Text(' 起/人', style: TextStyle(color: Color(0xFF909399), fontSize: 11)),
                    ],
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _SearchResultPage extends StatelessWidget {
  final List<dynamic> products;
  const _SearchResultPage({required this.products});

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('搜索结果')),
      body: ListView(padding: const EdgeInsets.all(16), children: products.map((p) => _ProductCard(product: p)).toList()),
    );
  }
}
